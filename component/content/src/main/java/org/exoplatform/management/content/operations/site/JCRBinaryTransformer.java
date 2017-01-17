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
package org.exoplatform.management.content.operations.site;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.mime.MimeTypes;
import org.exoplatform.management.common.DataTransformerPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * The Class JCRBinaryTransformer.
 */
public class JCRBinaryTransformer implements DataTransformerPlugin {

  /** The Constant BINARY_FILE_NAME_PREFIX. */
  public static final String BINARY_FILE_NAME_PREFIX = "Binary_";

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(JCRBinaryTransformer.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public void exportData(Object... objects) {
    if (objects == null || objects.length != 3) {
      throw new IllegalArgumentException("Some parameters are missing.");
    }
    if (!(objects[0] instanceof List)) {
      throw new IllegalArgumentException("First parameter should be of type java.util.List.");
    }
    if (!(objects[1] instanceof Node)) {
      throw new IllegalArgumentException("Second parameter should be a javax.jcr.Node.");
    }
    if (!(objects[2] instanceof String)) {
      throw new IllegalArgumentException("Third parameter should be a String.");
    }

    @SuppressWarnings("unchecked")
    List<ExportTask> exportTasks = (List<ExportTask>) objects[0];
    Node node = (Node) objects[1];
    String parentPath = (String) objects[2];

    try {
      if (!node.isNodeType("nt:file")) {
        return;
      }
    } catch (Exception e) {
      try {
        LOG.error("Error while cheking on nodetype of node " + node.getPath(), e);
      } catch (RepositoryException e1) {
        throw new RuntimeException(e);// throw e and not e1
      }
      return;
    }

    try {
      InputStream inputStream = node.getNode("jcr:content").getProperty("jcr:data").getStream();
      String filename = parentPath + "/" + BINARY_FILE_NAME_PREFIX + node.getName();
      if (filename.lastIndexOf('.') < filename.lastIndexOf('/')) {
        String mimeExtension = node.getNode("jcr:content").hasProperty("jcr:mimeType") ? node.getNode("jcr:content").getProperty("jcr:mimeType").getString() : ".data";
        try {
          mimeExtension = MimeTypes.getDefaultMimeTypes().forName(mimeExtension).getExtension();
          if (StringUtils.isEmpty(mimeExtension)) {
            mimeExtension = ".data";
          }
        } catch (Exception e) {}
        filename += mimeExtension;
      }
      exportTasks.add(new JCRBinaryExportTask(filename, inputStream));
    } catch (Exception e) {
      try {
        LOG.error("Error while copying content of node " + node.getPath(), e);
      } catch (RepositoryException e1) {
        throw new RuntimeException(e);// throw e and not e1
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void importData(Object... objects) {}

  /**
   * The Class JCRBinaryExportTask.
   */
  public static class JCRBinaryExportTask implements ExportTask {
    
    /** The filename. */
    String filename;
    
    /** The input stream. */
    InputStream inputStream;

    /**
     * Instantiates a new JCR binary export task.
     *
     * @param filename the filename
     * @param inputStream the input stream
     */
    public JCRBinaryExportTask(String filename, InputStream inputStream) {
      this.filename = filename;
      this.inputStream = inputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEntry() {
      return filename;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void export(OutputStream outputStream) throws IOException {
      try {
        LOG.info("Export file: " + filename);
        IOUtils.copy(inputStream, outputStream);
      } finally {
        if (inputStream != null) {
          inputStream.close();
        }
      }
    }
  }
}