package org.exoplatform.management.packaging.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.exoplatform.management.packaging.Util.PackagingUtil;

/**
 * Created with IntelliJ IDEA. User: gregorysebert Date: 11/07/12 Time: 15:38 To
 * change this template use File | Settings | File Templates.
 */
public class PackageContent {

	public PackageContent(String zipPath, File tmpFolder) {
		try {
			
            PackagingUtil.extractZip(zipPath, tmpFolder);


		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
