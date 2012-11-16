package org.exoplatform.management.packaging.operations;

import java.io.File;

import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.ResultHandler;
import org.exoplatform.management.packaging.task.PackageContent;
import org.exoplatform.management.packaging.task.PackageMop;
import org.exoplatform.management.packaging.task.PackageTask;
import org.gatein.management.api.operation.model.ExportResourceModel;
import org.exoplatform.management.packaging.Util.PackagingUtil;




/**
 * Created with IntelliJ IDEA.
 * User: gregorysebert
 * Date: 11/07/12
 * Time: 10:34
 * To change this template use File | Settings | File Templates.
 */
public class ExtensionExportResource implements OperationHandler
{
	 
	
    private String tmpFolderPath;

	@Override
    public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException
    {
        try {

            OperationAttributes myAttributes = operationContext.getAttributes();
            String[] options = myAttributes.getValue("filter").split(",");

            String tempFolder = System.getProperty("java.io.tmpdir");

			File temp = new File(tempFolder + "/exploded-extension");
            if (temp.exists()&& temp.isDirectory())
            {
                PackagingUtil.deleteDir(temp);
            }

            temp = new File(tempFolder + "/exploded-extension");
            temp.mkdir();
			tmpFolderPath = temp.getPath();
			
			
			//create folder tree in temp folder :
			// tmpFolder
			//	|__WEB-INF
			//	|	|__web.xml
			//	|	|__conf
			// 	|	|	|__mop
			//	|	|	|	|__portal-configuration.xml
			//	|	|	|	|__exportMopZip
			// 	|	|	|__content
			//	|	|	|	|__wcm-content-configuration.xml
			//	|	|	|	|__exportContentZip
			//	|	|	|__configuration.xml

			
			//create WEB-INF
			File webInfFolder=new File(temp.getPath()+"/WEB-INF");
			webInfFolder.mkdir();
			
			//create conf
			File confFolder=new File(webInfFolder.getPath()+"/conf");
			confFolder.mkdir();
			
			
			
			
			
            for (int i=0; i<options.length;i++)
                {
                   String[] option = options[i].split(":");

                   if (option.length == 2)
                   {
                        if (option[0].equals("mop")) {
                        	//create mop
                			File mopFolder=new File(confFolder.getPath()+"/mop");
                			mopFolder.mkdir();
                			PackageMop packageMop = new PackageMop(option[1], mopFolder);
                        }
                        else if (option[0].equals("content"))   {
                        	//create content
                			File contentFolder=new File(confFolder.getPath()+"/content");
                			contentFolder.mkdir();
                        	PackageContent packageContent = new PackageContent(option[1],contentFolder);
                        }
                        else throw new OperationException(OperationNames.EXPORT_RESOURCE, "Invalid option  : " + option.toString());
                   }
                   else throw new OperationException(OperationNames.EXPORT_RESOURCE, "Invalid option  : " + option.toString());
                   }

            	
            PackageTask task = new PackageTask(temp);
			resultHandler.completed(new ExportResourceModel(task));
            	
            
        } catch(Exception e) {
            throw new OperationException(OperationNames.EXPORT_RESOURCE, "Unable to create extension, options are missing or invalid");
        }
    }

	
}
