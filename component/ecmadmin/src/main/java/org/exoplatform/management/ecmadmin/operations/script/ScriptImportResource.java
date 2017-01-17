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
package org.exoplatform.management.ecmadmin.operations.script;

import org.apache.commons.io.IOUtils;
import org.exoplatform.management.ecmadmin.operations.ECMAdminImportResource;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The Class ScriptImportResource.
 *
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class ScriptImportResource extends ECMAdminImportResource {
  
  /** The Constant log. */
  private static final Log log = ExoLogger.getLogger(ScriptImportResource.class);
  
  /** The script service. */
  private ScriptService scriptService = null;

  /**
   * Instantiates a new script import resource.
   */
  public ScriptImportResource() {
    super(null);
  }

  /**
   * Instantiates a new script import resource.
   *
   * @param filePath the file path
   */
  public ScriptImportResource(String filePath) {
    super(filePath);
  }

  /**
   * {@inheritDoc}
   */
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    // get attributes and attachement inputstream
    super.execute(operationContext, resultHandler);

    if (scriptService == null) {
      scriptService = operationContext.getRuntimeContext().getRuntimeComponent(ScriptService.class);
    }

    SessionProvider systemSessionProvider = SessionProvider.createSystemProvider();
    ZipInputStream zin = new ZipInputStream(attachmentInputStream);
    try {
      ZipEntry ze = null;
      while ((ze = zin.getNextEntry()) != null) {
        try {
          if (!ze.getName().startsWith("ecmadmin/script/")) {
            continue;
          }
          String path = ze.getName().substring("ecmadmin/script/".length());
          CmsScript script = null;
          try {
            script = scriptService.getScript(path);
          } catch (ClassNotFoundException exception) {
            // Script doesn't exist
          }
          if (script != null) {
            if (replaceExisting) {
              log.info("Overwrite existing script '" + path + "'.");
            } else {
              log.info("Ignore existing script'" + path + "'.");
              continue;
            }
          }
          String data = IOUtils.toString(zin, "UTF-8");
          scriptService.addScript(path, data, systemSessionProvider);
        } finally {
          zin.closeEntry();
        }
      }
      resultHandler.completed(NoResultModel.INSTANCE);
    } catch (Exception exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while importing ECMS scripts.", exception);
    } finally {
      try {
        zin.close();
      } catch (IOException e) {
        throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while closing stream.", e);
      }
    }
  }
}