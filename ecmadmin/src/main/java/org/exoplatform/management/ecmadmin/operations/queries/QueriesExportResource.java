package org.exoplatform.management.ecmadmin.operations.queries;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.query.Query;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.cms.queries.impl.QueryData;
import org.exoplatform.services.cms.queries.impl.QueryPlugin;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 * @version $Revision$
 */
public class QueriesExportResource implements OperationHandler {

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    try {
      QueryService queryService = operationContext.getRuntimeContext().getRuntimeComponent(QueryService.class);
      OrganizationService organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);

      List<ExportTask> exportTasks = new ArrayList<ExportTask>();

      // shared queries
      List<Node> sharedQueries = queryService.getSharedQueries(WCMCoreUtils.getSystemSessionProvider());

      Configuration configurationSharedQueries = new Configuration();
      if (sharedQueries != null && !sharedQueries.isEmpty()) {
        ExternalComponentPlugins externalComponentPlugins = new ExternalComponentPlugins();
        externalComponentPlugins.setTargetComponent(QueryService.class.getName());
        ArrayList<ComponentPlugin> componentPluginsList = new ArrayList<ComponentPlugin>();
        externalComponentPlugins.setComponentPlugins(componentPluginsList);

        ComponentPlugin queriesComponentPlugin = new ComponentPlugin();
        queriesComponentPlugin.setName("query.plugin");
        queriesComponentPlugin.setSetMethod("setQueryPlugin");
        queriesComponentPlugin.setType(QueryPlugin.class.getName());

        InitParams queriesPluginInitParams = new InitParams();
        queriesComponentPlugin.setInitParams(queriesPluginInitParams);
        componentPluginsList.add(queriesComponentPlugin);

        // Queries API returns Node object instead of QueryData, so we need
        // to convert them...
        for (Node sharedQueryNode : sharedQueries) {
          QueryData queryData = new QueryData();
          queryData.setName(sharedQueryNode.getProperty("exo:name").getString());
          queryData.setStatement(sharedQueryNode.getProperty("jcr:statement").getString());
          queryData.setLanguage(sharedQueryNode.getProperty("jcr:language").getString());
          queryData.setCacheResult(sharedQueryNode.getProperty("exo:cachedResult").getBoolean());
          Value[] permissionsValues = sharedQueryNode.getProperty("exo:accessPermissions").getValues();
          List<String> permissions = new ArrayList<String>();
          for (Value permissionValue : permissionsValues) {
            permissions.add(permissionValue.getString());
          }
          queryData.setPermissions(permissions);

          ObjectParameter objectParam = new ObjectParameter();
          objectParam.setName(queryData.getName());
          objectParam.setObject(queryData);
          queriesPluginInitParams.addParam(objectParam);
        }
        configurationSharedQueries.addExternalComponentPlugins(externalComponentPlugins);
      }
      exportTasks.add(new QueriesExportTask(configurationSharedQueries, null));

      // users queries
      ListAccess<User> usersListAccess = organizationService.getUserHandler().findAllUsers();
      User[] users = usersListAccess.load(0, usersListAccess.getSize());
      for (User user : users) {
        List<Query> userQueries = queryService.getQueries(user.getUserName(), WCMCoreUtils.getSystemSessionProvider());

        if (userQueries != null && !userQueries.isEmpty()) {
          Configuration configurationUserQueries = new Configuration();

          ExternalComponentPlugins userQueriesExternalComponentPlugins = new ExternalComponentPlugins();
          userQueriesExternalComponentPlugins.setTargetComponent(QueryService.class.getName());
          ArrayList<ComponentPlugin> userQueriesComponentPluginsList = new ArrayList<ComponentPlugin>();
          userQueriesExternalComponentPlugins.setComponentPlugins(userQueriesComponentPluginsList);

          ComponentPlugin userQueriesComponentPlugin = new ComponentPlugin();
          userQueriesComponentPlugin.setName("query.plugin");
          userQueriesComponentPlugin.setSetMethod("setQueryPlugin");
          userQueriesComponentPlugin.setType(QueryPlugin.class.getName());

          InitParams userQueriesPluginInitParams = new InitParams();
          userQueriesComponentPlugin.setInitParams(userQueriesPluginInitParams);
          userQueriesComponentPluginsList.add(userQueriesComponentPlugin);

          for (Query query : userQueries) {
            QueryData queryData = new QueryData();
            String queryPath = query.getStoredQueryPath();
            queryData.setName(queryPath.substring(queryPath.lastIndexOf("/") + 1));
            queryData.setStatement(query.getStatement());
            queryData.setLanguage(query.getLanguage());
            queryData.setCacheResult(false);
            // no permissions are set on users' queries
            queryData.setPermissions(null);

            ObjectParameter objectParam = new ObjectParameter();
            objectParam.setName(queryData.getName());
            objectParam.setObject(queryData);
            userQueriesPluginInitParams.addParam(objectParam);
          }

          configurationUserQueries.addExternalComponentPlugins(userQueriesExternalComponentPlugins);
          exportTasks.add(new QueriesExportTask(configurationUserQueries, user.getUserName()));
        }
      }

      resultHandler.completed(new ExportResourceModel(exportTasks));
    } catch (Exception e) {
      throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to retrieve the list of the contents sites : " + e.getMessage());
    }
  }
}
