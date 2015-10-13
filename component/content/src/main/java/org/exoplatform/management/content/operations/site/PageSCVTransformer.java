package org.exoplatform.management.content.operations.site;

import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.common.DataTransformerPlugin;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationType;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;

public class PageSCVTransformer implements DataTransformerPlugin {

  private static final Log LOG = ExoLogger.getLogger(PageSCVTransformer.class);

  @Override
  public void exportData(Object... objects) {
    if (objects == null || objects.length == 0) {
      LOG.warn("Can't transform data of empty pages.");
      return;
    }
    for (Object object : objects) {
      if (object != null && object instanceof Page) {
        Page page = (Page) object;
        convert(page.getChildren());
      } else {
        LOG.warn("Can't convert object of type " + object.getClass());
      }
    }
  }

  @Override
  public void importData(Object... objects) {}

  private static void convert(List<?> children) {
    if ((children == null) || (children.size() == 0)) {
      return;
    }
    for (Object child : children) {
      if (child instanceof Container) {
        Container container = (Container) child;
        convert(container.getChildren());
      } else if (child instanceof Application && ((Application<?>) child).getType().equals(ApplicationType.PORTLET)) {
        @SuppressWarnings("unchecked")
        Application<Portlet> application = (Application<Portlet>) child;
        try {
          Portlet portlet = getPreferences(application);

          String nodeIdentifier = portlet == null || portlet.getPreference("nodeIdentifier") == null ? null : portlet.getPreference("nodeIdentifier").getValue();
          if (!StringUtils.isEmpty(nodeIdentifier) && !nodeIdentifier.startsWith("/")) {
            String repository = portlet == null ? null : portlet.getPreference("repository").getValue();
            if (repository == null) {
              repository = WCMCoreUtils.getRepository().getConfiguration().getName();
            }
            String workspace = portlet == null ? null : portlet.getPreference("workspace").getValue();
            Node node = getRealNode(repository, workspace, nodeIdentifier);

            LOG.info("Transform 'nodeIdentifier' parameter from'" + nodeIdentifier + "' to '" + node.getPath() + "'");

            portlet.setValue("nodeIdentifier", node.getPath());
          }
        } catch (Exception e) {
          LOG.error("Can't convert SCV application preferences " + application.getId(), e);
        }
      }
    }
  }

  private static Node getRealNode(String repository, String workspace, String identifier) throws Exception {
    RepositoryService repositoryService = (RepositoryService) PortalContainer.getInstance().getComponentInstanceOfType(RepositoryService.class);
    Node Node = AbstractOperationHandler.getSession(repositoryService, workspace).getNodeByUUID(identifier);
    return Utils.getRealNode(Node);
  }

  private static Portlet getPreferences(Application<Portlet> application) throws Exception {
    DataStorage dataStorage = (DataStorage) PortalContainer.getInstance().getComponentInstanceOfType(DataStorage.class);
    return dataStorage.load(application.getState(), application.getType());
  }
}