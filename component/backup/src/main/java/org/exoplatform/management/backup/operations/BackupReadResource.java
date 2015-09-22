package org.exoplatform.management.backup.operations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class BackupReadResource extends AbstractOperationHandler {

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException,
      OperationException {
    repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);

    String repositoryName = operationContext.getAddress().resolvePathTemplate("repository");
    String workspaceName = operationContext.getAddress().resolvePathTemplate("workspace");

    if (repositoryName == null) {
      Set<String> children = new LinkedHashSet<String>();

      List<RepositoryEntry> repositories = repositoryService.getConfig().getRepositoryConfigurations();
      for (RepositoryEntry repositoryEntry : repositories) {
        children.add(repositoryEntry.getName());
      }
      resultHandler.completed(new ReadResourceModel("repositories", children));
    } else if (workspaceName == null) {
      Set<String> children = new LinkedHashSet<String>();

      ManageableRepository repository = null;
      try {
        repository = repositoryService.getRepository(repositoryName);
      } catch (Exception e) {
        throw new OperationException(OperationNames.READ_RESOURCE, "error while retrieving repository object", e);
      }
      String[] wsNames = repository.getWorkspaceNames();
      for (String wsName : wsNames) {
        children.add(wsName);
      }
      resultHandler.completed(new ReadResourceModel("workspaces", children));
    } else {
      resultHandler.completed(NoResultModel.INSTANCE);
    }
  }
}
