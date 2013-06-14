package org.exoplatform.management.gadget.tasks;

import java.io.IOException;
import java.io.OutputStream;

import javax.jcr.Session;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class GadgetExportTask implements ExportTask {
  private String workspaceName;
  private String gadgetJCRPath;
  private String gadgetName;
  private ManageableRepository manageableRepository;

  public GadgetExportTask(String gadgetName, ManageableRepository manageableRepository, String workspaceName, String jcrPath) {
    this.workspaceName = workspaceName;
    this.gadgetName = gadgetName;
    this.gadgetJCRPath = jcrPath + "app:" + gadgetName;
    this.manageableRepository = manageableRepository;
  }

  
  public String getEntry() {
    return gadgetName + ".xml";
  }

  
  public void export(OutputStream outputStream) throws IOException {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      Session session = sessionProvider.getSession(workspaceName, manageableRepository);
      session.exportSystemView(gadgetJCRPath, outputStream, false, false);
      outputStream.flush();
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting gadget data", exception);
    }
  }

}
