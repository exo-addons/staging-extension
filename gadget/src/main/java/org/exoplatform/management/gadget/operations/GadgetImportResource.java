package org.exoplatform.management.gadget.operations;

import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Session;

import org.exoplatform.application.gadget.Gadget;
import org.exoplatform.application.gadget.GadgetRegistryService;
import org.exoplatform.application.registry.impl.ApplicationRegistryChromatticLifeCycle;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class GadgetImportResource implements OperationHandler {

  private static final Log log = ExoLogger.getLogger(GadgetImportResource.class);
  private static final String DEFAULT_JCR_PATH = "/production/app:gadgets/";

  private ChromatticManager chromatticManager;
  private RepositoryService repositoryService;
  private GadgetRegistryService gadgetRegistryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {

    if (chromatticManager == null) {
      chromatticManager = operationContext.getRuntimeContext().getRuntimeComponent(ChromatticManager.class);
      if (chromatticManager == null) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "ChromatticManager doesn't exist.");
      }
    }
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
      if (repositoryService == null) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "RepositoryService doesn't exist.");
      }
    }
    if (gadgetRegistryService == null) {
      gadgetRegistryService = operationContext.getRuntimeContext().getRuntimeComponent(GadgetRegistryService.class);
      if (gadgetRegistryService == null) {
        throw new OperationException(OperationNames.EXPORT_RESOURCE, "GadgetRegistryService doesn't exist.");
      }
    }
    OperationAttributes attributes = operationContext.getAttributes();

    String jcrPath = null;
    boolean replaceExisting = true;
    if (attributes != null && attributes.getValues("filter") != null && !attributes.getValues("filter").isEmpty()) {
      Iterator<String> filters = attributes.getValues("filter").iterator();
      while (filters.hasNext()) {
        String filter = filters.next();
        if (filter.startsWith("jcrpath:")) {
          jcrPath = filter.substring("jcrpath:".length());
          if (!jcrPath.endsWith("/")) {
            jcrPath += "/";
          }
        }
        if (filter.startsWith("replaceExisting:")) {
          replaceExisting = Boolean.parseBoolean(filter.substring("replaceExisting:".length()));
        }
      }
    }
    if (jcrPath == null) {
      jcrPath = DEFAULT_JCR_PATH;
    }

    ApplicationRegistryChromatticLifeCycle lifeCycle = (ApplicationRegistryChromatticLifeCycle) chromatticManager
        .getLifeCycle("app");
    String workspaceName = lifeCycle.getWorkspaceName();

    OperationAttachment attachment = operationContext.getAttachment(false);
    InputStream attachmentInputStream = attachment.getStream();
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for Gadget import.");
    }
    final ZipInputStream zis = new ZipInputStream(attachmentInputStream);
    ZipEntry entry;
    Session session = null;
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      SessionProvider sessionProvider = SessionProvider.createSystemProvider();
      session = sessionProvider.getSession(workspaceName, manageableRepository);
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = entry.getName();

        // Skip directories
        // & Skip empty entries
        // & Skip entries not in sites/zip
        if (entry.isDirectory() || filePath.equals("") || !filePath.endsWith(".xml")) {
          continue;
        }

        String gadgetName = filePath.substring(0, filePath.length() - 4);
        Gadget gadget = gadgetRegistryService.getGadget(gadgetName);

        if (gadget != null) {
          if (replaceExisting) {
            log.info(gadgetName + " already exists. filter used 'replaceExisting:true' (default filter value is true).");
            gadgetRegistryService.removeGadget(gadgetName);

            // commit changes
            RequestLifeCycle.end();
            // keep an active transaction
            RequestLifeCycle.begin(PortalContainer.getInstance());
          } else {
            log.info(gadgetName + "already exists. Ignore gadget.");
          }
        }
        session.importXML(jcrPath, zis, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        session.save();
        zis.closeEntry();
      }
      zis.close();
    } catch (Exception e) {
      throw new OperationException(operationContext.getOperationName(), "Error while importing Gadgets", e);
    } finally {
      if (session != null && session.isLive()) {
        session.logout();
      }
    }

    resultHandler.completed(NoResultModel.INSTANCE);
  }

}