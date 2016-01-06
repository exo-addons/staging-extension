package org.exoplatform.management.gadget.operations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.application.registry.impl.ApplicationRegistryChromatticLifeCycle;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.gadget.tasks.GadgetExportTask;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class GadgetExportResource extends AbstractOperationHandler {

  private static final String DEFAULT_JCR_PATH = "/production/app:gadgets/";
  private ChromatticManager chromatticManager;
  private RepositoryService repositoryService;

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

    PathAddress address = operationContext.getAddress();
    String gadgetName = address.resolvePathTemplate("gadget-name");
    OperationAttributes attributes = operationContext.getAttributes();

    String jcrPath = null;
    if (attributes != null && attributes.getValues("filter") != null && !attributes.getValues("filter").isEmpty()) {
      Iterator<String> filters = attributes.getValues("filter").iterator();
      while (filters.hasNext() && jcrPath == null) {
        String filter = filters.next();
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

    ApplicationRegistryChromatticLifeCycle lifeCycle = (ApplicationRegistryChromatticLifeCycle) chromatticManager.getLifeCycle("app");
    String workspaceName = lifeCycle.getWorkspaceName();

    increaseCurrentTransactionTimeOut(operationContext);
    try {
      ManageableRepository manageableRepository = repositoryService.getDefaultRepository();
      List<ExportTask> exportTasks = new ArrayList<ExportTask>();
      exportTasks.add(new GadgetExportTask(gadgetName, manageableRepository, workspaceName, jcrPath));
      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(operationContext.getOperationName(), "Error while exporting Gadget" + gadgetName, e);
    } finally {
      restoreDefaultTransactionTimeOut(repositoryService);
    }
  }

}