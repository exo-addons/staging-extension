/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.management.forum.operations;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ForumExportTask implements ExportTask {
  private static final Log log = ExoLogger.getLogger(ForumExportTask.class);

  private final RepositoryService repositoryService;
  private final String type;
  private final boolean recursive;
  private final String workspace;
  private final String absolutePath;
  private final String categoryId;
  private final String forumId;

  public ForumExportTask(RepositoryService repositoryService, String type, String categoryId, String forumId, String workspace, String absolutePath, boolean recursive) {
    this.categoryId = categoryId;
    this.forumId = forumId;
    this.type = type;
    this.recursive = recursive;
    this.repositoryService = repositoryService;
    this.workspace = workspace;
    this.absolutePath = absolutePath;
  }

  @Override
  public String getEntry() {
    return getEntryPath(type, forumId == null || forumId.isEmpty() ? categoryId : forumId, absolutePath);
  }

  public static String getEntryPath(String type, String id, String absolutePath) {
    return new StringBuilder("forum/").append(type).append("/").append(id).append(absolutePath).append(".xml").toString();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    Session session = null;
    try {
      log.info("Export: " + workspace + ":" + absolutePath);

      session = getSession(workspace);
      session.exportDocumentView(absolutePath, outputStream, false, !recursive);
    } catch (RepositoryException exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to export content from : " + absolutePath, exception);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  private Session getSession(String workspace) throws RepositoryException, LoginException, NoSuchWorkspaceException {
    SessionProvider provider = SessionProvider.createSystemProvider();
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = provider.getSession(workspace, repository);
    return session;
  }
}
