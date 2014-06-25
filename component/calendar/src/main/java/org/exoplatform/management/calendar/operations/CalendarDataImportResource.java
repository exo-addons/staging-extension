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
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.management.calendar.CalendarExtension;
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

  private boolean groupCalendar;
  private String type;

  public CalendarDataImportResource(boolean groupCalendar) {
    this.groupCalendar = groupCalendar;
    type = groupCalendar ? CalendarExtension.GROUP_CALENDAR_TYPE : CalendarExtension.PERSONAL_CALENDAR_TYPE;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    CalendarService calendarService = operationContext.getRuntimeContext().getRuntimeComponent(CalendarService.class);

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

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
    try {
      // extract data from zip
      tempFolderPath = extractDataFromZip(attachmentInputStream, contentsByOwner);

      for (String tempFilePath : contentsByOwner) {
        importCalendar(calendarService, tempFilePath, replaceExisting);
      }

    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unable to import calendar contents", e);
    } finally {
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

  private void importCalendar(CalendarService calendarService, String tempFilePath, boolean replaceExisting) throws Exception {
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
        if (!filePath.endsWith(".xml") && !filePath.contains(CalendarExportTask.CALENDAR_SEPARATOR)) {
          log.warn("Uknown file format found at location: '" + filePath + "'. Ignore it.");
          continue;
        }

        log.info("Receiving content " + filePath);

        // Put XML Export file in temp folder
        copyToDisk(zis, targetFolderPath + filePath);

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
