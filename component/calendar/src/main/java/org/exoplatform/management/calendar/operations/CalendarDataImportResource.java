package org.exoplatform.management.calendar.operations;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.management.calendar.CalendarExtension;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.url.PortalURLContext;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.PortalURL;
import org.exoplatform.web.url.ResourceType;
import org.exoplatform.web.url.URLFactory;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class CalendarDataImportResource implements OperationHandler {

  final private static Logger log = LoggerFactory.getLogger(CalendarDataImportResource.class);

  final private static int BUFFER = 2048000;

  private SpaceService spaceService;
  private UserACL userACL;

  private boolean groupCalendar;
  private boolean spaceCalendar;
  private String type;

  public CalendarDataImportResource(boolean groupCalendar, boolean spaceCalendar) {
    this.groupCalendar = groupCalendar;
    this.spaceCalendar = spaceCalendar;
    type = groupCalendar ? spaceCalendar ? CalendarExtension.SPACE_CALENDAR_TYPE : CalendarExtension.GROUP_CALENDAR_TYPE : CalendarExtension.PERSONAL_CALENDAR_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    CalendarService calendarService = operationContext.getRuntimeContext().getRuntimeComponent(CalendarService.class);
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    UserPortalConfigService portalConfigService = operationContext.getRuntimeContext().getRuntimeComponent(UserPortalConfigService.class);

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

    // "create-space" attribute. Defaults to false.
    boolean createSpace = filters.contains("create-space:true");

    OperationAttachment attachment = operationContext.getAttachment(false);
    if (attachment == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No attachment available for calendar import.");
    }

    InputStream attachmentInputStream = attachment.getStream();
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for calendar import.");
    }

    String tempFolderPath = null;
    Set<String> contentsByOwner = new HashSet<String>();

    RequestContext originalRequestContext = WebuiRequestContext.getCurrentInstance();
    try {
      // extract data from zip
      tempFolderPath = extractDataFromZip(attachmentInputStream, contentsByOwner);

      // FIXME: INTEG-333. Add this to not have a null pointer exception while
      // importing
      if (!contentsByOwner.isEmpty()) {
        if (originalRequestContext == null) {
          final ControllerContext controllerContext = new ControllerContext(null, null, new MockHttpServletRequest(), new MockHttpServletResponse(), null);
          PortalRequestContext portalRequestContext = new PortalRequestContext((WebuiApplication) null, controllerContext, groupCalendar ? SiteType.GROUP.getName() : SiteType.PORTAL.getName(),
              portalConfigService.getDefaultPortal(), "/portal/" + portalConfigService.getDefaultPortal() + "/calendar", (Locale) null) {
            @Override
            public <R, U extends PortalURL<R, U>> U newURL(ResourceType<R, U> resourceType, URLFactory urlFactory) {
              if (resourceType.equals(NodeURL.TYPE)) {
                @SuppressWarnings("unchecked")
                U u = (U) new NodeURL(new PortalURLContext(controllerContext, null) {
                  public <S extends Object, V extends org.exoplatform.web.url.PortalURL<S, V>> String render(V url) {
                    return "";
                  };
                });
                return u;
              }
              return super.newURL(resourceType, urlFactory);
            }
          };
          WebuiRequestContext.setCurrentInstance(portalRequestContext);
        }
      }

      for (String tempFilePath : contentsByOwner) {
        importCalendar(calendarService, tempFolderPath, tempFilePath, replaceExisting, createSpace);
      }

    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unable to import calendar contents", e);
    } finally {
      WebuiRequestContext.setCurrentInstance(originalRequestContext);
      if (tempFolderPath != null) {
        try {
          FileUtils.deleteDirectory(new File(tempFolderPath));
        } catch (IOException e) {
          log.warn("Unable to delete temp folder: " + tempFolderPath + ". Not blocker.", e);
        }
      }

      if (attachmentInputStream != null) {
        try {
          attachmentInputStream.close();
        } catch (IOException e) {
          // Nothing to do
        }
      }
    }
    resultHandler.completed(NoResultModel.INSTANCE);
  }

  /**
   * Extract data from zip
   * 
   * @param attachment
   * @return
   */
  public String extractDataFromZip(InputStream attachmentInputStream, Set<String> contentsByCalendar) throws Exception {
    File tmpZipFile = null;
    String targetFolderPath = null;
    try {
      tmpZipFile = copyAttachementToLocalFolder(attachmentInputStream);

      // Get path of folder where to unzip files
      targetFolderPath = tmpZipFile.getAbsolutePath().replaceAll("\\.zip$", "") + "/";

      // Organize File paths by id and extract files from zip to a temp
      // folder
      extractFilesById(tmpZipFile, targetFolderPath, contentsByCalendar);
    } finally {
      if (tmpZipFile != null) {
        try {
          FileUtils.forceDelete(tmpZipFile);
        } catch (Exception e) {
          log.warn("Unable to delete temp file: " + tmpZipFile.getAbsolutePath() + ". Not blocker.", e);
          tmpZipFile.deleteOnExit();
        }
      }
    }
    return targetFolderPath;
  }

  private File copyAttachementToLocalFolder(InputStream attachmentInputStream) throws IOException, FileNotFoundException {
    NonCloseableZipInputStream zis = null;
    File tmpZipFile = null;
    try {
      // Copy attachement to local File
      tmpZipFile = File.createTempFile("staging-calendar", ".zip");
      tmpZipFile.deleteOnExit();
      FileOutputStream tmpFileOutputStream = new FileOutputStream(tmpZipFile);
      IOUtils.copy(attachmentInputStream, tmpFileOutputStream);
      tmpFileOutputStream.close();
      attachmentInputStream.close();
    } finally {
      if (zis != null) {
        try {
          zis.reallyClose();
        } catch (IOException e) {
          log.warn("Can't close inputStream of attachement.");
        }
      }
    }
    return tmpZipFile;
  }

  private boolean createSpaceIfNotExists(String tempFolderPath, String spacePrettyName, String groupId, boolean createSpace) throws IOException {
    Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
    if (space == null && createSpace) {
      FileInputStream spaceMetadataFile = new FileInputStream(tempFolderPath + "/" + SpaceMetadataExportTask.getEntryPath(spacePrettyName));
      try {
        // Unmarshall metadata xml file
        XStream xstream = new XStream();
        xstream.alias("metadata", SpaceMetaData.class);
        SpaceMetaData spaceMetaData = (SpaceMetaData) xstream.fromXML(spaceMetadataFile);

        log.info("Automatically create new space: '" + spaceMetaData.getPrettyName() + "'.");
        space = new Space();
        space.setPrettyName(spaceMetaData.getPrettyName());
        space.setDisplayName(spaceMetaData.getDisplayName());
        space.setGroupId(groupId);
        space.setTag(spaceMetaData.getTag());
        space.setApp(spaceMetaData.getApp());
        space.setEditor(spaceMetaData.getEditor() != null ? spaceMetaData.getEditor() : spaceMetaData.getManagers().length > 0 ? spaceMetaData.getManagers()[0] : userACL.getSuperUser());
        space.setManagers(spaceMetaData.getManagers());
        space.setInvitedUsers(spaceMetaData.getInvitedUsers());
        space.setRegistration(spaceMetaData.getRegistration());
        space.setDescription(spaceMetaData.getDescription());
        space.setType(spaceMetaData.getType());
        space.setVisibility(spaceMetaData.getVisibility());
        space.setPriority(spaceMetaData.getPriority());
        space.setUrl(spaceMetaData.getUrl());
        spaceService.createSpace(space, space.getEditor());
        return true;
      } finally {
        if (spaceMetadataFile != null) {
          try {
            spaceMetadataFile.close();
          } catch (Exception e) {
            log.warn(e);
          }
        }
      }
    }
    return (space != null);
  }

  private void importCalendar(CalendarService calendarService, String tempFolderPath, String tempFilePath, boolean replaceExisting, boolean createSpace) throws Exception {
    // Unmarshall calendar data file
    XStream xStream = new XStream();
    xStream.alias("Calendar", Calendar.class);
    xStream.alias("Event", CalendarEvent.class);

    @SuppressWarnings("unchecked")
    List<Object> objects = (List<Object>) xStream.fromXML(FileUtils.readFileToString(new File(tempFilePath)));

    Calendar calendar = (Calendar) objects.get(0);
    Calendar toReplaceCalendar = calendarService.getCalendarById(calendar.getId());
    if (toReplaceCalendar != null) {
      if (replaceExisting) {
        log.info("Overwrite existing calendar: " + toReplaceCalendar.getName());
        if (groupCalendar) {
          calendarService.removePublicCalendar(calendar.getId());
          calendarService.savePublicCalendar(calendar, true);
        } else {
          calendarService.removeUserCalendar(calendar.getCalendarOwner(), calendar.getId());
          calendarService.saveUserCalendar(calendar.getCalendarOwner(), calendar, true);
        }
      } else {
        log.info("Ignore existing calendar: " + toReplaceCalendar.getName());
      }
    } else {
      log.info("Create calendar: " + calendar.getName());
      if (groupCalendar) {
        if (spaceCalendar) {
          String groupId = calendar.getCalendarOwner();
          String spacePrettyName = groupId.replace("/spaces/", "");

          boolean spaceCreatedOrAlreadyExists = createSpaceIfNotExists(tempFolderPath, spacePrettyName, groupId, createSpace);
          if (!spaceCreatedOrAlreadyExists) {
            log.warn("Import of Calendar of space '" + spacePrettyName + "' is ignored. Turn on 'create-space:true' option if you want to automatically create the space.");
            return;
          }
          calendarService.removePublicCalendar(calendar.getId());
        }
        calendarService.savePublicCalendar(calendar, true);
      } else {
        calendarService.saveUserCalendar(calendar.getCalendarOwner(), calendar, true);
      }
    }
    @SuppressWarnings("unchecked")
    List<CalendarEvent> events = (List<CalendarEvent>) objects.get(1);
    for (CalendarEvent event : events) {
      log.info("Create calendar event: " + calendar.getName() + "/" + event.getSummary());
      if (groupCalendar) {
        calendarService.savePublicEvent(calendar.getId(), event, true);
      } else {
        calendarService.saveUserEvent(calendar.getCalendarOwner(), calendar.getId(), event, true);
      }
    }
  }

  private void extractFilesById(File tmpZipFile, String targetFolderPath, Set<String> contentsByOwner) throws Exception {
    // Open an input stream on local zip file
    NonCloseableZipInputStream zis = new NonCloseableZipInputStream(new FileInputStream(tmpZipFile));

    try {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = entry.getName();
        // Skip entries not managed by this extension
        if (filePath.equals("") || !filePath.startsWith("calendar/" + type + "/")) {
          continue;
        }

        // Skip directories
        if (entry.isDirectory()) {
          // Create directory in unzipped folder location
          createFile(new File(targetFolderPath + filePath), true);
          continue;
        }

        // Skip non managed
        if (!filePath.endsWith(".xml") && !filePath.endsWith(SpaceMetadataExportTask.FILENAME) && !filePath.contains(CalendarExportTask.CALENDAR_SEPARATOR)) {
          log.warn("Uknown file format found at location: '" + filePath + "'. Ignore it.");
          continue;
        }

        log.info("Receiving content " + filePath);

        // Put XML Export file in temp folder
        copyToDisk(zis, targetFolderPath + filePath);

        // Skip metadata file
        if (filePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
          continue;
        }

        contentsByOwner.add(targetFolderPath + filePath);
      }
    } finally {
      if (zis != null) {
        zis.reallyClose();
      }
    }
  }

  // Bug in SUN's JDK XMLStreamReader implementation closes the underlying
  // stream when
  // it finishes reading an XML document. This is no good when we are using
  // a
  // ZipInputStream.
  // See http://bugs.sun.com/view_bug.do?bug_id=6539065 for more
  // information.
  public static class NonCloseableZipInputStream extends ZipInputStream {
    public NonCloseableZipInputStream(InputStream inputStream) {
      super(inputStream);
    }

    @Override
    public void close() throws IOException {}

    private void reallyClose() throws IOException {
      super.close();
    }
  }

  private static void copyToDisk(InputStream input, String output) throws Exception {
    byte data[] = new byte[BUFFER];
    BufferedOutputStream dest = null;
    try {
      FileOutputStream fileOuput = new FileOutputStream(createFile(new File(output), false));
      dest = new BufferedOutputStream(fileOuput, BUFFER);
      int count = 0;
      while ((count = input.read(data, 0, BUFFER)) != -1)
        dest.write(data, 0, count);
    } finally {
      if (dest != null) {
        dest.close();
      }
    }
  }

  private static File createFile(File file, boolean folder) throws Exception {
    if (file.getParentFile() != null)
      createFile(file.getParentFile(), true);
    if (file.exists())
      return file;
    if (file.isDirectory() || folder)
      file.mkdir();
    else
      file.createNewFile();
    return file;
  }

}
