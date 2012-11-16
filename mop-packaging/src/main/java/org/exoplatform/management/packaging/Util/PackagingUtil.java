package org.exoplatform.management.packaging.Util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: gregorysebert
 * Date: 11/07/12
 * Time: 18:02
 * To change this template use File | Settings | File Templates.
 */
public class PackagingUtil {

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    public static void extractZip(String zipPath, File destFolder)
            throws FileNotFoundException, IOException {
        ZipInputStream zipInputStream;
        ZipEntry zipEntry;
        byte[] buffer = new byte[2048];
        zipInputStream = new ZipInputStream(new FileInputStream(zipPath));
        zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            String fileName = zipEntry.getName();
            if (zipEntry.isDirectory()) {
                File newDir = new File(destFolder.getPath() + "/" + fileName);
                newDir.mkdir();
            } else {
                if (fileName.split("/").length > 1)
                {
                    String[] dirs = fileName.split("/");
                    String tempDir = "";

                    for(int i=0;i<dirs.length-1;i++)
                    {
                        tempDir += "/"+dirs[i];
                    }

                    File newDir = new File(destFolder.getPath() + tempDir);
                    if (!newDir.exists()) newDir.mkdirs();
                }
                FileOutputStream fileoutputstream = new FileOutputStream(
                        destFolder.getPath() + "/" + fileName);
                int n;

                while ((n = zipInputStream.read(buffer, 0, 2048)) > -1) {
                    fileoutputstream.write(buffer, 0, n);
                }

                fileoutputstream.close();
            }
            zipEntry = zipInputStream.getNextEntry();
        }

        zipInputStream.close();
    }
}
