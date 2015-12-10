package org.exoplatform.management.ecmadmin.operations.view;

import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Node;

import org.apache.tika.io.IOUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 */
public class ViewImportResource extends ECMAdminImportResource {
  private static final Log log = ExoLogger.getLogger(ViewImportResource.class);
  private static ManageViewService viewService = null;
  private static NodeHierarchyCreator nodeHierarchyCreator;

  public ViewImportResource() {
    this(null);
  }

  public ViewImportResource(String filePath) {
    super(filePath);
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    if (viewService == null) {
      viewService = operationContext.getRuntimeContext().getRuntimeComponent(ManageViewService.class);
    }
    if (nodeHierarchyCreator == null) {
      nodeHierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);
    }

    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    String templatesHomePath = getTemplatesHomePath() + "/";

    final ZipInputStream zin = new ZipInputStream(attachmentInputStream);
    ZipEntry entry;
    try {
      while ((entry = zin.getNextEntry()) != null) {
        String filePath = entry.getName();
        if (!filePath.startsWith("ecmadmin/view/")) {
          continue;
        }
        // Skip directories
        // & Skip empty entries
        // & Skip entries not in sites/zip
        if (entry.isDirectory() || filePath.trim().isEmpty() || !(filePath.endsWith(".gtmpl") || filePath.endsWith(".xml"))) {
          continue;
        }
        if (filePath.endsWith(".gtmpl")) {
          String templateName = extractTemplateName(filePath);

          log.debug("Reading Stream for template: " + templateName);
          String content = IOUtils.toString(zin);

          Node template = null;
          try {
            template = viewService.getTemplate(templatesHomePath + templateName, sessionProvider);
          } catch (Exception e) {
            // template does not exist, ignore the error
          }

          if (template != null) {
            if (replaceExisting) {
              log.info("Overwrite existing view template: " + templateName);
              viewService.updateTemplate(templateName, content, templatesHomePath, sessionProvider);
            } else {
              log.info("Ignore existing view template: " + templateName);
            }
          } else {
            log.info("Add new view template: " + templateName);
            viewService.addTemplate(templateName, content, templatesHomePath, sessionProvider);
          }

        } else if (filePath.endsWith(".xml")) {
          log.debug("Parsing : " + filePath);

          InitParams initParams = Utils.fromXML(IOUtils.toByteArray(zin), InitParams.class);
          @SuppressWarnings("unchecked")
          Iterator<ObjectParameter> iterator = initParams.getObjectParamIterator();
          while (iterator.hasNext()) {
            ObjectParameter objectParameter = (ObjectParameter) iterator.next();
            if (!(objectParameter.getObject() instanceof ViewConfig)) {
              continue;
            }
            ViewConfig config = (ViewConfig) objectParameter.getObject();
            if (viewService.hasView(config.getName())) {
              if (replaceExisting) {
                log.info("Overwrite existing view: " + config.getName());
                viewService.removeView(config.getName());
                viewService.addView(config.getName(), config.getPermissions(), config.isHideExplorerPanel(), config.getTemplate(), config.getTabList());
              } else {
                log.info("Ignore existing view: " + config.getName());
              }
            } else {
              log.info("Add new view: " + config.getName());
              viewService.addView(config.getName(), config.getPermissions(), config.getTemplate(), config.getTabList());
            }
          }
        }
        zin.closeEntry();
      }
      zin.close();
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception e) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while reading View Templates from Stream.", e);
    }
  }

  private String getTemplatesHomePath() {
    return nodeHierarchyCreator.getJcrPath(BasePath.ECM_EXPLORER_TEMPLATES);
  }

  private String extractTemplateName(String filePath) {
    return filePath.replace("ecmadmin/view/templates/", "").replace(".gtmpl", "");
  }

}
