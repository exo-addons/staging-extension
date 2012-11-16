package org.exoplatform.management.packaging.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import org.exoplatform.management.packaging.xml.XmlConfiguration;
import org.gatein.management.api.operation.ResultHandler;
import org.exoplatform.management.packaging.Util.PackagingUtil;

/**
 * Created with IntelliJ IDEA. User: gregorysebert Date: 11/07/12 Time: 15:38 To
 * change this template use File | Settings | File Templates.
 */
public class PackageMop {

	public PackageMop(String zipPath, File tmpFolder) {
		try {
			
           PackagingUtil.extractZip(zipPath, tmpFolder);

            File portalConfig=new File(tmpFolder.getPath()+"/portal-configuration.xml");
            XmlConfiguration xmlConfiguration = new   XmlConfiguration();
            xmlConfiguration.addPortalConfiguration(new FileOutputStream(portalConfig));

        } catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
