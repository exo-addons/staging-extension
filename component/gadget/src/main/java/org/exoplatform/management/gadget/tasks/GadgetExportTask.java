package org.exoplatform.management.gadget.tasks;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.model.ExportTask;

import javax.jcr.Session;
import java.io.IOException;
import java.io.OutputStream;

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

  @Override
  public String getEntry() {
    return "gadget/" + gadgetName + ".xml";
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      Session session = sessionProvider.getSession(workspaceName, manageableRepository);
      session.exportDocumentView(gadgetJCRPath, outputStream, false, false);
      outputStream.flush();
    } catch (Exception exception) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Error while exporting gadget data", exception);
    }
  }

}
