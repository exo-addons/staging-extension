package org.exoplatform.management.calendar.operations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.chromattic.common.collection.Collections;
import org.exoplatform.calendar.service.Calendar;
import org.exoplatform.calendar.service.CalendarEvent;
import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.impl.CalendarServiceImpl;
import org.exoplatform.calendar.service.impl.JCRDataStorage;
import org.exoplatform.management.calendar.CalendarExtension;
import org.exoplatform.management.common.AbstractImportOperationHandler;
import org.exoplatform.management.common.MockHttpServletRequest;
import org.exoplatform.management.common.MockHttpServletResponse;
import org.exoplatform.management.common.activities.ActivitiesExportTask;
import org.exoplatform.management.common.activities.SpaceMetadataExportTask;
import org.exoplatform.management.common.api.ActivityImportOperationInterface;
import org.exoplatform.management.common.api.FileEntry;
import org.exoplatform.management.common.api.FileImportOperationInterface;
import org.exoplatform.management.common.api.NavigationUtils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.url.PortalURLContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
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
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class CalendarDataImportResource extends AbstractImportOperationHandler implements ActivityImportOperationInterface, FileImportOperationInterface {
  final private static Logger log = LoggerFactory.getLogger(CalendarDataImportResource.class);

  public static final String EVENT_ID_KEY = "EventID";
  public static final String CALENDAR_PORTLET_NAME = "CalendarPortlet";
  public static final String INVITATION_DETAIL = "/invitation/detail/";
  public static final String EVENT_LINK_KEY = "EventLink";

  private CalendarService calendarService;
  private JCRDataStorage calendarStorage;
  private UserPortalConfigService portalConfigService;
  private PageService pageService;
  private DataStorage dataStorage;

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
    calendarService = operationContext.getRuntimeContext().getRuntimeComponent(CalendarService.class);
    calendarStorage = ((CalendarServiceImpl) calendarService).getDataStorage();
    spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
    activityManager = operationContext.getRuntimeContext().getRuntimeComponent(ActivityManager.class);
    activityStorage = operationContext.getRuntimeContext().getRuntimeComponent(ActivityStorage.class);
    identityStorage = operationContext.getRuntimeContext().getRuntimeComponent(IdentityStorage.class);
    userACL = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
    portalConfigService = operationContext.getRuntimeContext().getRuntimeComponent(UserPortalConfigService.class);
    pageService = operationContext.getRuntimeContext().getRuntimeComponent(PageService.class);
    dataStorage = operationContext.getRuntimeContext().getRuntimeComponent(DataStorage.class);

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");
    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");
    // "create-space" attribute. Defaults to false.
    boolean createSpace = filters.contains("create-space:true");

    InputStream attachmentInputStream = getAttachementInputStream(operationContext);

    RequestContext originalRequestContext = null;
    try {
      // extract data from zip
      Map<String, List<FileEntry>> contentsByOwner = extractDataFromZip(attachmentInputStream);

      // FIXME: INTEG-333. Add this to not have a null pointer exception while
      // importing
      if (!contentsByOwner.isEmpty()) {
        originalRequestContext = fixPortalRequest();
      }

      for (String categoryId : contentsByOwner.keySet()) {
        List<FileEntry> fileEntries = contentsByOwner.get(categoryId);
        FileEntry spaceMetadataFile = getAndRemoveFileByPath(fileEntries, SpaceMetadataExportTask.FILENAME);
        FileEntry activitiesFile = getAndRemoveFileByPath(fileEntries, ActivitiesExportTask.FILENAME);
        for (FileEntry fileEntry : fileEntries) {
          importCalendar(fileEntry.getFile(), spaceMetadataFile == null ? null : spaceMetadataFile.getFile(), replaceExisting, createSpace);
        }
        if (activitiesFile != null) {
          String spaceGroupId = activitiesFile.getNodePath();
          Space space = spaceService.getSpaceByGroupId(spaceGroupId);
          importActivities(activitiesFile.getFile(), space.getPrettyName(), true);
        }
      }
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Unable to import calendar contents", e);
    } finally {
      if (originalRequestContext != null) {
        WebuiRequestContext.setCurrentInstance(originalRequestContext);
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

  @Override
  public void attachActivityToEntity(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (comment != null) {
      return;
    }
    String eventId = activity.getTemplateParams().get(EVENT_ID_KEY);
    CalendarEvent event = calendarService.getEventById(eventId);
    saveEvent(event, activity);
  }

  @Override
  public boolean isActivityNotValid(ExoSocialActivity activity, ExoSocialActivity comment) throws Exception {
    if (comment != null) {
      return false;
    }
    String eventId = activity.getTemplateParams().get(EVENT_ID_KEY);
    if (eventId == null) {
      log.warn("An unkown Calendar activity was found: " + activity.getTitle());
      return true;
    }
    CalendarEvent event = calendarService.getEventById(eventId);
    if (event == null) {
      log.warn("Calendar event not found. Cannot import activity '" + activity.getTitle() + "'.");
      return true;
    }
    return false;
  }

  private void importCalendar(File file, File spaceMetadataFile, boolean replaceExisting, boolean createSpace) throws Exception {
    // Unmarshall calendar data file
    XStream xStream = new XStream();
    xStream.alias("Calendar", Calendar.class);
    xStream.alias("Event", CalendarEvent.class);

    @SuppressWarnings("unchecked")
    List<Object> objects = (List<Object>) xStream.fromXML(FileUtils.readFileToString(file, "UTF-8"));

    Calendar calendar = (Calendar) objects.get(0);
    if (groupCalendar && (calendar.getCalendarOwner() == null || !calendar.getCalendarOwner().startsWith(SpaceUtils.SPACE_GROUP))) {
      String[] groups = calendar.getGroups();
      if (groups != null) {
        for (String groupId : groups) {
          if (groupId != null && groupId.startsWith(SpaceUtils.SPACE_GROUP)) {
            calendar.setCalendarOwner(groups[0]);
            break;
          }
        }
      }
    }

    Calendar toReplaceCalendar = calendarService.getCalendarById(calendar.getId());
    if (toReplaceCalendar != null) {
      if (replaceExisting) {
        log.info("Overwrite existing calendar: " + toReplaceCalendar.getName());
        if (groupCalendar) {
          // FIXME event activities aren't deleted
          List<CalendarEvent> events = calendarService.getGroupEventByCalendar(Collections.list(calendar.getId()));
          deleteCalendarActivities(events);
          // Delete Calendar
          calendarService.removePublicCalendar(calendar.getId());
        } else {
          // FIXME event activities aren't deleted
          List<CalendarEvent> events = calendarService.getUserEventByCalendar(calendar.getCalendarOwner(), Collections.list(calendar.getId()));
          deleteCalendarActivities(events);
          // Delete Calendar
          calendarService.removeUserCalendar(calendar.getCalendarOwner(), calendar.getId());
        }
      } else {
        log.info("Ignore existing calendar: " + toReplaceCalendar.getName());
      }
    }

    log.info("Create calendar: " + calendar.getName());
    if (groupCalendar) {
      if (spaceCalendar) {
        if (spaceMetadataFile != null && spaceMetadataFile.exists()) {
          boolean spaceCreatedOrAlreadyExists = createSpaceIfNotExists(spaceMetadataFile, createSpace);
          if (!spaceCreatedOrAlreadyExists) {
            log.warn("Import of Calendar of space '" + calendar.getName() + "' is ignored. Turn on 'create-space:true' option if you want to automatically create the space.");
            return;
          }
          calendarService.removePublicCalendar(calendar.getId());
        }
      }
      calendarStorage.savePublicCalendar(calendar, true, null);
    } else {
      calendarStorage.saveUserCalendar(calendar.getCalendarOwner(), calendar, true);
    }

    @SuppressWarnings("unchecked")
    List<CalendarEvent> events = (List<CalendarEvent>) objects.get(1);
    for (CalendarEvent event : events) {
      log.info("Create calendar event: " + calendar.getName() + "/" + event.getSummary());
      if (groupCalendar) {
        calendarStorage.savePublicEvent(calendar.getId(), event, true);
      } else {
        calendarStorage.saveUserEvent(calendar.getCalendarOwner(), calendar.getId(), event, true);
      }
    }
    deleteCalendarActivities(events);
  }

  private void deleteCalendarActivities(List<CalendarEvent> events) {
    for (CalendarEvent event : events) {
      if (event != null && event.getActivityId() != null) {
        deleteActivity(event.getActivityId());
      }
    }
  }

  private void saveEvent(CalendarEvent event, ExoSocialActivity exoSocialActivity) throws Exception {
    Calendar calendar = calendarService.getCalendarById(event.getCalendarId());
    if (calendar.getCalendarOwner().startsWith("/")) {
      calendarStorage.savePublicEvent(event.getCalendarId(), event, false);
      // the URL of Stream Activity will use staging URL, modify it
      updateCalendarActivityURL(event, spaceCalendar ? calendar.getCalendarOwner() : null);
    } else {
      calendarStorage.saveUserEvent(userACL.getSuperUser(), event.getCalendarId(), event, false);
      // the URL of Stream Activity will use staging URL, modify it
      updateCalendarActivityURL(event, null);
    }
  }

  private void updateCalendarActivityURL(CalendarEvent event, String groupId) throws Exception {
    ExoSocialActivity activity = activityManager.getActivity(event.getActivityId());
    if (activity != null) {
      Map<String, String> templateParams = activity.getTemplateParams();
      if (templateParams.containsKey(EVENT_LINK_KEY)) {
        templateParams.put(EVENT_LINK_KEY, getLink(event, activity, groupId));
        activityManager.updateActivity(activity);
      }
    }
  }

  private String getLink(CalendarEvent event, ExoSocialActivity activity, String spaceGroupId) throws Exception {
    SiteKey siteKey = null;
    if (spaceGroupId == null) {
      siteKey = SiteKey.portal(portalConfigService.getDefaultPortal());
    } else {
      siteKey = SiteKey.group(spaceGroupId);
    }
    PortalRequestContext prc = Util.getPortalRequestContext();
    UserPortal userPortal = prc.getUserPortal();
    String uri = NavigationUtils.getNavURIWithApplication(pageService, dataStorage, userPortal, siteKey, CALENDAR_PORTLET_NAME);
    if (uri != null) {
      String username = ConversationState.getCurrent().getIdentity().getUserId();
      String url = uri + INVITATION_DETAIL + username + "/" + event.getId() + "/" + event.getCalType();
      return url;
    }
    return StringUtils.EMPTY;
  }

  private RequestContext fixPortalRequest() {
    RequestContext originalRequestContext;
    originalRequestContext = WebuiRequestContext.getCurrentInstance();
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
    return originalRequestContext;
  }

  @Override
  public String getManagedFilesPrefix() {
    return "calendar/" + type + "/";
  }

  @Override
  public boolean isUnKnownFileFormat(String filePath) {
    return !filePath.contains(CalendarExportTask.CALENDAR_SEPARATOR)
        || (!filePath.endsWith(".xml") && !filePath.endsWith(SpaceMetadataExportTask.FILENAME) && !filePath.endsWith(ActivitiesExportTask.FILENAME));
  }

  @Override
  public boolean addSpecialFile(List<FileEntry> fileEntries, String filePath, File file) {
    if (filePath.endsWith(SpaceMetadataExportTask.FILENAME)) {
      fileEntries.add(new FileEntry(SpaceMetadataExportTask.FILENAME, file));
      return true;
    } else if (filePath.endsWith(ActivitiesExportTask.FILENAME)) {
      fileEntries.add(new FileEntry(ActivitiesExportTask.FILENAME, file));
      return true;
    }
    return false;
  }

  @Override
  public String extractIdFromPath(String path) {
    String[] paths = path.split(CalendarExportTask.CALENDAR_SEPARATOR);
    path = paths[1];
    String id = path.substring(0, path.contains(".") ? path.indexOf(".") : path.contains("/") ? path.indexOf("/") : path.length());
    return id;
  }

  @Override
  public String getNodePath(String filePath) {
    String[] paths = filePath.split(CalendarExportTask.CALENDAR_SEPARATOR);
    return paths[1];
  }
}