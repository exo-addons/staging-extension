package org.exoplatform.management.ecmadmin.operations.queries;

import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.cms.queries.impl.QueryData;
import org.exoplatform.services.cms.queries.impl.QueryPlugin;
import org.exoplatform.services.cms.queries.impl.QueryServiceImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;

/**
 * @author <a href="mailto:thomas.delhomenie@exoplatform.com">Thomas
 *         Delhoménie</a>
 */
public class QueriesImportResource extends ECMAdminImportResource {
  private static final Log log = ExoLogger.getLogger(QueriesImportResource.class);
  private QueryService queryService;
  private RepositoryService repositoryService;
  private DMSConfiguration dmsConfiguration;
  private NodeHierarchyCreator nodeHierarchyCreator;

  public QueriesImportResource() {
    super(null);
  }

  public QueriesImportResource(String filePath) {
    super(filePath);
  }

  
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    if (dmsConfiguration == null) {
      dmsConfiguration = operationContext.getRuntimeContext().getRuntimeComponent(DMSConfiguration.class);
    }
    if (repositoryService == null) {
      repositoryService = operationContext.getRuntimeContext().getRuntimeComponent(RepositoryService.class);
    }
    if (queryService == null) {
      queryService = operationContext.getRuntimeContext().getRuntimeComponent(QueryService.class);
    }
    if (nodeHierarchyCreator == null) {
      nodeHierarchyCreator = operationContext.getRuntimeContext().getRuntimeComponent(NodeHierarchyCreator.class);
    }

    try {
      ZipInputStream zin = new ZipInputStream(attachmentInputStream);
      ZipEntry ze = null;

      IBindingFactory bfact = BindingDirectory.getFactory(Configuration.class);
      IUnmarshallingContext uctx = bfact.createUnmarshallingContext();

      Session session = getDMSJCRSession();
      String baseQueriesPath = nodeHierarchyCreator.getJcrPath(BasePath.QUERIES_PATH);
      while ((ze = zin.getNextEntry()) != null) {
        String zipEntryName = ze.getName();
        if (!zipEntryName.startsWith("queries/")) {
          continue;
        }

        Configuration configuration = (Configuration) uctx.unmarshalDocument(zin, "UTF-8");
        ExternalComponentPlugins externalComponentPlugins = configuration.getExternalComponentPlugins(QueryService.class
            .getName());
        List<ComponentPlugin> componentPlugins = externalComponentPlugins.getComponentPlugins();

        // Users' queries
        if (zipEntryName.startsWith("queries/users/") && zipEntryName.endsWith("-queries-configuration.xml")) {
          // extract username from filename
          String username = zipEntryName.substring(zipEntryName.lastIndexOf("/") + 1,
              zipEntryName.indexOf("-queries-configuration.xml"));

          List<Query> queries = queryService.getQueries(username, WCMCoreUtils.getSystemSessionProvider());

          // Can't create user's queries via configuration (only shared),
          // so they are directly created
          // via the queryService.addQuery method
          for (ComponentPlugin componentPlugin : componentPlugins) {
            @SuppressWarnings("rawtypes")
            Iterator objectParamIterator = componentPlugin.getInitParams().getObjectParamIterator();
            while (objectParamIterator.hasNext()) {
              ObjectParameter objectParam = (ObjectParameter) objectParamIterator.next();
              Object object = objectParam.getObject();
              if (object instanceof QueryData) {
                QueryData queryData = (QueryData) object;
                boolean alreadyExists = false;
                for (Query query : queries) {
                  if (queryData.getName().equals(
                      query.getStoredQueryPath().substring(query.getStoredQueryPath().lastIndexOf("/") + 1))) {
                    if (replaceExisting) {
                      log.info("Overwrite query '" + queryData.getName() + "' already exists for user '" + username + "'.");
                      Node queriesNode = nodeHierarchyCreator.getUserNode(WCMCoreUtils.getSystemSessionProvider(), username)
                          .getNode(queryService.getRelativePath());
                      Node queryNode = queriesNode.getNode(queryData.getName());
                      queryNode.remove();
                      queriesNode.getSession().save();
                    } else {
                      log.info("Ignore existing query '" + queryData.getName() + "' for user '" + username + "'.");
                      alreadyExists = true;
                      break;
                    }
                  }
                }
                if (!alreadyExists || replaceExisting) {
                  queryService.addQuery(queryData.getName(), queryData.getStatement(), queryData.getLanguage(), username);
                }
              }
            }
          }
        } else if (zipEntryName.endsWith("shared-queries-configuration.xml")) {
          for (ComponentPlugin componentPlugin : componentPlugins) {
            Class<?> pluginClass = Class.forName(componentPlugin.getType());
            @SuppressWarnings("rawtypes")
            Iterator objectParamIterator = componentPlugin.getInitParams().getObjectParamIterator();
            while (objectParamIterator.hasNext()) {
              ObjectParameter objectParam = (ObjectParameter) objectParamIterator.next();
              Object object = objectParam.getObject();
              if (object instanceof QueryData) {
                QueryData queryData = (QueryData) object;
                // if the shared query already exists, remove it from the
                // init-params of the plugin
                if (session.itemExists(baseQueriesPath + "/" + queryData.getName())) {
                  if (replaceExisting) {
                    log.info("Overwrite shared query '" + queryData.getName() + "'.");
                    session.getItem(baseQueriesPath + "/" + queryData.getName()).remove();
                    session.save();
                  } else {
                    log.info("Ignore existing shared query '" + queryData.getName() + "'.");
                    componentPlugin.getInitParams().removeParameter(queryData.getName());
                  }
                }
              }
            }
            QueryPlugin cplugin = (QueryPlugin) PortalContainer.getInstance().createComponent(pluginClass,
                componentPlugin.getInitParams());
            cplugin.setName(componentPlugin.getName());
            cplugin.setDescription(componentPlugin.getDescription());
            // TODO add setQueryPlugin in Interface QueryService
            ((QueryServiceImpl) queryService).setQueryPlugin(cplugin);
          }
        }
        zin.closeEntry();
      }
      zin.close();
      // init service, so it will create the shared queries
      queryService.init();
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing ECMS queries.", exception);
    }
  }

  private Session getDMSJCRSession() throws Exception {
    SessionProvider provider = WCMCoreUtils.getSystemSessionProvider();
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration.getConfig();
    return provider.getSession(dmsRepoConfig.getSystemWorkspace(), manageableRepository);
  }

}
