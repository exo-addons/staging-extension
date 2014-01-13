package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;

import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class ApplicationTemplatesReadResource implements OperationHandler {

  public final static String DISPLAY_TEMPLATE_CATEGORY = "navigation";

  public final static String DISPLAY_TEMPLATE_LIST = "list";

  public final static String TEMPLATE_STORAGE_FOLDER = "content-list-viewer";

  public final static String CONTENT_LIST_TYPE = "ContentList";

  public final static String CATEGORIES_CONTENT_TYPE = "CategoryContents";

  public final static String CATOGORIES_NAVIGATION_TYPE = "CategoryNavigation";

  public final static String PAGINATOR_TEMPLATE_CATEGORY = "paginators";

  private ApplicationTemplateManagerService templateManagerService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    String operationName = operationContext.getOperationName();
    PathAddress address = operationContext.getAddress();

    String applicationName = address.resolvePathTemplate("application-name");
    if (applicationName == null) {
      throw new OperationException(operationName, "No application name specified.");
    }

    Set<String> templates = new TreeSet<String>();
    templateManagerService = operationContext.getRuntimeContext().getRuntimeComponent(ApplicationTemplateManagerService.class);
    try {
      Set<String> tmpTemplates = new TreeSet<String>();
      tmpTemplates.addAll(getTemplateList(TEMPLATE_STORAGE_FOLDER, DISPLAY_TEMPLATE_LIST));
      tmpTemplates.addAll(getTemplateList(TEMPLATE_STORAGE_FOLDER, DISPLAY_TEMPLATE_CATEGORY));
      tmpTemplates.addAll(getTemplateList(TEMPLATE_STORAGE_FOLDER, PAGINATOR_TEMPLATE_CATEGORY));
      for (String template : tmpTemplates) {
        templates.add(template.substring(template.indexOf(TEMPLATE_STORAGE_FOLDER) + TEMPLATE_STORAGE_FOLDER.length() + 1));
      }

    } catch (Exception e) {
      throw new OperationException("Read template applications", "Error while retrieving applications with templates", e);
    }
    resultHandler.completed(new ReadResourceModel("Available application templates", templates));
  }

  private List<String> getTemplateList(String portletName, String category) throws Exception {
    List<String> templateOptionList = new ArrayList<String>();
    List<Node> templateNodeList = templateManagerService.getTemplatesByCategory(portletName, category, WCMCoreUtils.getUserSessionProvider());
    for (Node templateNode : templateNodeList) {
      templateOptionList.add(templateNode.getPath());
    }
    return templateOptionList;
  }
}