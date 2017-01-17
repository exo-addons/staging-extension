/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.management.service.api;

import org.apache.commons.io.IOUtils;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.impl.UnmarshallingContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * The Class Utils.
 */
public class Utils {
  
  /** The log. */
  private static Log log = ExoLogger.getLogger(Utils.class);
  
  /** The Constant CONFIGURATION_FILE_XSD. */
  private static final String CONFIGURATION_FILE_XSD = "<configuration " + "\r\n   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
      + "\r\n   xsi:schemaLocation=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\""
      + "\r\n   xmlns=\"http://www.exoplatform.org/xml/ns/kernel_1_2.xsd\">";

  /**
   * Write configuration.
   *
   * @param zos the zos
   * @param entryName the entry name
   * @param configuration the configuration
   * @return true, if successful
   */
  public static boolean writeConfiguration(ZipOutputStream zos, String entryName, Configuration configuration) {
    try {
      if (entryName.startsWith("/")) {
        entryName = entryName.substring(1);
      }
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(toXML(configuration));
      zos.closeEntry();
    } catch (Exception e) {
      log.error("Error while writing file " + entryName, e);
      return false;
    }
    return true;
  }

  /**
   * Write configuration.
   *
   * @param zos the zos
   * @param entryName the entry name
   * @param externalComponentPlugins the external component plugins
   * @return true, if successful
   */
  public static boolean writeConfiguration(ZipOutputStream zos, String entryName, ExternalComponentPlugins... externalComponentPlugins) {
    Configuration configuration = new Configuration();
    for (ExternalComponentPlugins externalComponentPlugin : externalComponentPlugins) {
      configuration.addExternalComponentPlugins(externalComponentPlugin);
    }
    try {
      if (entryName.startsWith("/")) {
        entryName = entryName.substring(1);
      }
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(toXML(configuration));
      zos.closeEntry();
    } catch (Exception e) {
      log.error("Error while writing file " + entryName, e);
      return false;
    }
    return true;
  }

  /**
   * Write zip enry.
   *
   * @param zos the zos
   * @param entryName the entry name
   * @param content the content
   */
  public static void writeZipEnry(ZipOutputStream zos, String entryName, String content) {
    try {
      if (entryName.startsWith("/")) {
        entryName = entryName.substring(1);
      }
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(content.getBytes("UTF-8"));
      zos.closeEntry();
    } catch (Exception e) {
      log.error("Error while writing file " + entryName, e);
    }
  }

  /**
   * Write zip enry.
   *
   * @param zos the zos
   * @param entryName the entry name
   * @param inputStream the input stream
   */
  public static void writeZipEnry(ZipOutputStream zos, String entryName, InputStream inputStream) {
    try {
      if (entryName.startsWith("/")) {
        entryName = entryName.substring(1);
      }
      zos.putNextEntry(new ZipEntry(entryName));
      zos.write(IOUtils.toByteArray(inputStream));
      zos.closeEntry();
    } catch (Exception e) {
      log.error("Error while writing file " + entryName, e);
    }
  }

  /**
   * To XML.
   *
   * @param obj the obj
   * @return the byte[]
   * @throws Exception the exception
   */
  public static byte[] toXML(Object obj) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IBindingFactory bfact = BindingDirectory.getFactory(obj.getClass());
    IMarshallingContext mctx = bfact.createMarshallingContext();
    mctx.setIndent(2);
    mctx.marshalDocument(obj, "UTF-8", null, out);
    String content = new String(out.toByteArray());
    content = content.replace("<configuration>", CONFIGURATION_FILE_XSD);
    content = content.replaceAll("<field name=\"([A-z])*\"/>", "");
    return content.getBytes();
  }

  /**
   * From XML.
   *
   * @param <T> the generic type
   * @param bytes the bytes
   * @param clazz the clazz
   * @return the t
   * @throws Exception the exception
   */
  public static <T> T fromXML(byte[] bytes, Class<T> clazz) throws Exception {
    ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
    IBindingFactory bfact = BindingDirectory.getFactory(clazz);
    UnmarshallingContext uctx = (UnmarshallingContext) bfact.createUnmarshallingContext();
    Object obj = uctx.unmarshalDocument(baos, "UTF-8");
    return clazz.cast(obj);
  }

  /**
   * Copy zip enries.
   *
   * @param zin the zin
   * @param zos the zos
   * @param rootPathInTarget the root path in target
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void copyZipEnries(ZipInputStream zin, ZipOutputStream zos, String rootPathInTarget) throws IOException {
    if (rootPathInTarget == null) {
      rootPathInTarget = "";
    }
    ZipEntry entry;
    while ((entry = zin.getNextEntry()) != null) {
      if (entry.isDirectory() || !entry.getName().contains(".")) {
        continue;
      }
      String targetEntryName = rootPathInTarget + ("/") + entry.getName();
      while (targetEntryName.contains("//")) {
        targetEntryName = targetEntryName.replace("//", "/");
      }
      if (targetEntryName.startsWith("/")) {
        targetEntryName = targetEntryName.substring(1);
      }
      zos.putNextEntry(new ZipEntry(targetEntryName));
      IOUtils.copy(zin, zos);
    }
    zos.flush();
  }

}
