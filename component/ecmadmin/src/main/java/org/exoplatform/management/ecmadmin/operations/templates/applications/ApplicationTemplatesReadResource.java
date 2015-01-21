package org.exoplatform.management.ecmadmin.operations.templates.applications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.management.api.PathAddress;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class ApplicationTemplatesReadResource extends AbstractOperationHandler {

  public final static Map<String, String[]> APPLICATION_SUB_TEMPLATES_MAP = new HashMap<String, String[]>();
  static {
    APPLICATION_SUB_TEMPLATES_MAP.put("content-list-viewer", new String[]
      { "navigation", "list", "paginators" });
    APPLICATION_SUB_TEMPLATES_MAP.put("search", new String[]
      { "search-form", "search-page-layout", "search-paginator", "search-result" });
  }

  private ApplicationTemplateManagerService templateManagerService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    String operationName = operationContext.getOperationName();
    PathAddress address = operationContext.getAddress();

    String applicationName = address.resolvePathTemplate("application-name");
    String subApplicationName = null;
    String applicationTemplateName = null;
    if (applicationName != null && applicationName.contains("/")) {
      String[] arr = applicationName.split("/");
      if (applicationName.endsWith(".gtmpl")) {
        applicationTemplateName = arr[arr.length - 1];
      }
      subApplicationName = arr[1];
      applicationName = arr[0];
    }

    if (applicationName == null || !APPLICATION_SUB_TEMPLATES_MAP.containsKey(applicationName)) {
      throw new OperationException(operationName, "No application name specified.");
    }

    TreeSet<String> templates = new TreeSet<String>();
    // In case a template is selected
    if (applicationTemplateName == null) {
      templateManagerService = operationContext.getRuntimeContext().getRuntimeComponent(ApplicationTemplateManagerService.class);
      try {
        Set<String> tmpTemplates = new TreeSet<String>();
        if (subApplicationName == null) {
          String[] applicationSubTemplates = APPLICATION_SUB_TEMPLATES_MAP.get(applicationName);
          for (String applicationSubTemplate : applicationSubTemplates) {
            tmpTemplates.addAll(getTemplateList(applicationName, applicationSubTemplate));
          }
        } else {
          tmpTemplates.addAll(getTemplateList(applicationName, subApplicationName));
        }
        for (String template : tmpTemplates) {
          templates.add(template.substring(template.indexOf(applicationName) + applicationName.length() + 1));
        }
      } catch (Exception e) {
        throw new OperationException("Read template applications", "Error while retrieving applications with templates", e);
      }
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