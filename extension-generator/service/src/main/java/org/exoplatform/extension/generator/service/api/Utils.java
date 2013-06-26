package org.exoplatform.extension.generator.service.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.impl.UnmarshallingContext;

public class Utils {
  private static Log log = ExoLogger.getLogger(Utils.class);

  public static boolean writeConfiguration(ZipOutputStream zos, String entryName, Configuration configuration) {
    try {
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(toXML(configuration));
      zos.closeEntry();
    } catch (Exception e) {
      log.error("Error while writing file " + entryName, e);
      return false;
    }
    return true;
  }

  public static boolean writeConfiguration(ZipOutputStream zos, String entryName, ExternalComponentPlugins... externalComponentPlugins) {
    Configuration configuration = new Configuration();
    for (ExternalComponentPlugins externalComponentPlugin : externalComponentPlugins) {
      configuration.addExternalComponentPlugins(externalComponentPlugin);
    }
    try {
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(toXML(configuration));
      zos.closeEntry();
    } catch (Exception e) {
      log.error("Error while writing file " + entryName, e);
      return false;
    }
    return true;
  }

  public static void writeZipEnry(ZipOutputStream zos, String entryName, String content) {
    try {
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(content.getBytes("UTF-8"));
      zos.closeEntry();
    } catch (Exception e) {
      log.error("Error while writing file " + entryName, e);
    }
  }

  public static void writeZipEnry(ZipOutputStream zos, String entryName, InputStream inputStream) {
    try {
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(IOUtils.toByteArray(inputStream));
      zos.closeEntry();
    } catch (Exception e) {
      log.error("Error while writing file " + entryName, e);
    }
  }

  public static byte[] toXML(Object obj) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IBindingFactory bfact = BindingDirectory.getFactory(obj.getClass());
    IMarshallingContext mctx = bfact.createMarshallingContext();
    mctx.setIndent(2);
    mctx.marshalDocument(obj, "UTF-8", null, out);
    return out.toByteArray();
  }

  public static <T> T fromXML(byte[] bytes, Class<T> clazz) throws Exception {
    ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
    IBindingFactory bfact = BindingDirectory.getFactory(clazz);
    UnmarshallingContext uctx = (UnmarshallingContext) bfact.createUnmarshallingContext();
    Object obj = uctx.unmarshalDocument(baos, "UTF-8");
    return clazz.cast(obj);
  }

}
