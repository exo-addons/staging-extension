package org.exoplatform.management.gadget.operations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.exoplatform.application.gadget.Gadget;
import org.exoplatform.application.gadget.GadgetRegistryService;
import org.exoplatform.application.registry.impl.ApplicationRegistryChromatticLifeCycle;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class GadgetImportResource extends AbstractOperationHandler {

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

    increaseCurrentTransactionTimeOut(operationContext);

    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

    // "jcrpath" attribute.
    String jcrPath = null;
    if (attributes != null && attributes.getValues("filter") != null && !attributes.getValues("filter").isEmpty()) {
      Iterator<String> filtersIterator = attributes.getValues("filter").iterator();
      while (filtersIterator.hasNext()) {
        String filter = filtersIterator.next();
        if (filter.startsWith("jcrpath:")) {
          jcrPath = filter.substring("jcrpath:".length());
          if (!jcrPath.endsWith("/")) {
            jcrPath += "/";
          }
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
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for Gadget import");
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
        if (entry.isDirectory() || filePath.equals("") || !filePath.startsWith("gadget/") || !filePath.endsWith(".xml")) {
          continue;
        }

        String gadgetName = filePath.substring("gadget/".length(), filePath.length() - 4);
        Gadget gadget = gadgetRegistryService.getGadget(gadgetName);

        if (gadget != null) {
          if (replaceExisting) {
            log.info(gadgetName + " already exists. Replace it.");
            gadgetRegistryService.removeGadget(gadgetName);

            // commit changes
            RequestLifeCycle.end();
            // keep an active transaction
            RequestLifeCycle.begin(PortalContainer.getInstance());

            doImportGadget(jcrPath, zis, session);
          } else {
            log.info(gadgetName + " already exists. Ignore gadget (add the attribute replace-existing:true if you want to replace existing gadgets).");
          }
        } else {
          log.info("Import gadget " + gadgetName);
          doImportGadget(jcrPath, zis, session);
        }
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

  private void doImportGadget(String jcrPath, ZipInputStream zis, Session session) throws IOException, RepositoryException {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(IOUtils.toByteArray(zis));
    session.importXML(jcrPath, byteArrayInputStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
    session.save();
  }

}