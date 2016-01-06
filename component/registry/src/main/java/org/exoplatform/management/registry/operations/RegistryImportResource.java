package org.exoplatform.management.registry.operations;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Session;

import org.exoplatform.application.registry.Application;
import org.exoplatform.application.registry.ApplicationCategory;
import org.exoplatform.application.registry.ApplicationRegistryService;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.management.registry.tasks.ApplicationExportTask;
import org.exoplatform.management.registry.tasks.CategoryExportTask;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IUnmarshallingContext;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class RegistryImportResource extends AbstractOperationHandler {
  private static final Log log = ExoLogger.getLogger(RegistryImportResource.class);
  private ApplicationRegistryService applicationRegistryService;

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    if (applicationRegistryService == null) {
      applicationRegistryService = operationContext.getRuntimeContext().getRuntimeComponent(ApplicationRegistryService.class);
      if (applicationRegistryService == null) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Cannot get ApplicationRegistryService instance.");
      }
    }
    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");

    OperationAttachment attachment = operationContext.getAttachment(false);
    InputStream attachmentInputStream = attachment.getStream();
    if (attachmentInputStream == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for Registry import.");
    }
    final ZipInputStream zin = new ZipInputStream(attachmentInputStream);
    ZipEntry entry;
    Session session = null;
    try {
      while ((entry = zin.getNextEntry()) != null) {
        try {
          String filePath = entry.getName();

          // Skip directories
          // & Skip empty entries
          // & Skip entries not in sites/zip
          if (entry.isDirectory() || filePath.equals("") || !(filePath.startsWith(ApplicationExportTask.APPLICATION_FILE_BASE_PATH))
              || !(filePath.endsWith(ApplicationExportTask.APPLICATION_FILE_SUFFIX) || filePath.endsWith(CategoryExportTask.CATEGORY_FILE_SUFFIX))) {
            continue;
          }

          IBindingFactory bfact = BindingDirectory.getFactory(ObjectParameter.class);
          IUnmarshallingContext uctx = bfact.createUnmarshallingContext();
          ObjectParameter objectParameter = (ObjectParameter) uctx.unmarshalDocument(zin, "UTF-8");
          if (filePath.endsWith(ApplicationExportTask.APPLICATION_FILE_SUFFIX)) {
            Application application = (Application) objectParameter.getObject();
            createApplication(application, replaceExisting);
          } else {
            ApplicationCategory category = (ApplicationCategory) objectParameter.getObject();
            ApplicationCategory categoryFromRepo = applicationRegistryService.getApplicationCategory(category.getName());
            if (categoryFromRepo != null) {
              if (replaceExisting) {
                log.info("Replacing Category:  " + category.getName());
                applicationRegistryService.remove(categoryFromRepo);
                applicationRegistryService.save(category);
              } else {
                log.info("Category already exists:  " + category.getName() + ", set replace-existing=true if you want to override it.");
              }
            } else {
              applicationRegistryService.save(category);
            }
            List<Application> applications = category.getApplications();
            for (Application application : applications) {
              createApplication(application, replaceExisting);
            }
          }
        } finally {
          zin.closeEntry();
        }
      }
    } catch (Exception e) {
      throw new OperationException(operationContext.getOperationName(), "Error while importing application registry", e);
    } finally {
      try {
        zin.close();
      } catch (IOException e) {
        throw new OperationException(operationContext.getOperationName(), "Error while closing stream after import finished", e);
      }
      if (session != null && session.isLive()) {
        session.logout();
      }
    }

    resultHandler.completed(NoResultModel.INSTANCE);
  }

  private void createApplication(Application application, boolean replaceExisting) {
    ApplicationCategory category = applicationRegistryService.getApplicationCategory(application.getCategoryName());
    if (category == null) {
      log.warn("Category  '" + application.getCategoryName() + "' was not found, creating it.");
      category = new ApplicationCategory();
      category.setName(application.getCategoryName());
      category.setDisplayName(application.getCategoryName());
      category.setDescription(application.getCategoryName());
      List<String> permExprs = new ArrayList<String>();
      permExprs.add("Everyone");
      applicationRegistryService.save(category);
    }
    Application applicationFromRepo = applicationRegistryService.getApplication(application.getCategoryName(), application.getApplicationName());
    if (applicationFromRepo != null) {
      if (replaceExisting) {
        log.info("Replacing Application:  " + category.getName() + "/" + application.getApplicationName());
        applicationRegistryService.remove(applicationFromRepo);
        applicationRegistryService.save(category, application);
      } else {
        log.info("Application already exists:  " + category.getName() + "/" + application.getApplicationName() + ", set replace-existing=true if you want to override it.");
      }
    } else {
      applicationRegistryService.save(category, application);
    }
  }

}