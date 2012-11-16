package org.exoplatform.management.packaging.task;

import org.gatein.management.api.operation.model.ExportTask;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: gregorysebert
 * Date: 11/07/12
 * Time: 16:11
 * To change this template use File | Settings | File Templates.
 */
public class PackageTask implements ExportTask {
    private File file = null;

    public PackageTask(File tmpFolder)
    {
       this.file = tmpFolder;
    }

    @Override
    public String getEntry()
    {
      return "/platform-extension/";
    }


    @Override
    public void export(OutputStream outputStream) throws IOException

    {

        File[] children = file.listFiles();


        for (File child : children) {
            browse(child, (ZipOutputStream)outputStream);
		}


    }
    private void browse(File currentFile,ZipOutputStream zos) throws IOException {
		if (currentFile.isDirectory()) {
			File[] children = currentFile.listFiles();

			for (File child : children) {
                browse(child, zos);
			}

		} else {
			//file is a file

			byte[] buffer = new byte[4096*1024];
			FileInputStream in = new FileInputStream(currentFile);

            // Add ZIP entry to output stream.
            zos.putNextEntry(new ZipEntry(this.getEntry()+currentFile.getPath().replace(file.getPath(), "")));

            // Transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            // Complete the entry
            in.close();
            zos.closeEntry();


		}
	}
}
