package org.exoplatform.management.organization;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.organization.*;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

import javax.jcr.Node;
import javax.jcr.Session;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class OrganizationModelJCRContentExportTask implements ExportTask {
  public static final String GROUPS_PATH = "groupsPath";

  private final RepositoryService repositoryService;
  private final String jcrPath;
  private final String workspace;

  private StringBuilder serializationPath = new StringBuilder(50);

  public OrganizationModelJCRContentExportTask(RepositoryService repositoryService, Node node, Object organizationObject) throws Exception {
    this.repositoryService = repositoryService;
    this.jcrPath = node.getPath();
    this.workspace = node.getSession().getWorkspace().getName();

    serializationPath.append(OrganizationManagementExtension.PATH_ORGANIZATION).append("/");
    if (organizationObject instanceof User) {
      serializationPath.append(OrganizationManagementExtension.PATH_ORGANIZATION_USER)
              .append("/")
              .append(((User) organizationObject).getUserName())
              .append("/u_content.xml");
    } else if (organizationObject instanceof Group) {
      serializationPath.append(OrganizationManagementExtension.PATH_ORGANIZATION_GROUP)
              .append("/")
              .append(((Group) organizationObject).getId())
              .append("/g_content.xml");
    }
  }

  @Override
  public String getEntry() {
    return serializationPath.toString();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspace, manageableRepository);
      session.exportDocumentView(jcrPath, outputStream, false, false);
      outputStream.flush();
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting user's personnal JCR contents", exception);
    }
  }
}