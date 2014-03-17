/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.management.wiki.operations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Session;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.mow.core.api.MOWService;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationAttachment;
import org.gatein.management.api.operation.OperationAttributes;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.OperationHandler;
import org.gatein.management.api.operation.OperationNames;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.NoResultModel;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Mar 5, 2014  
 */
public class WikiDataImportResource implements OperationHandler {
  
  private static final Log log = ExoLogger.getLogger(WikiDataImportResource.class.getName());
  
  private WikiType wikiType;
  
  public WikiDataImportResource(WikiType wikiType) {
    this.wikiType = wikiType;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException,
                                                                                     OperationException {
    
    MOWService mowService = operationContext.getRuntimeContext()
        .getRuntimeComponent(MOWService.class);
    
    OperationAttributes attributes = operationContext.getAttributes();
    List<String> filters = attributes.getValues("filter");

    // "replace-existing" attribute. Defaults to false.
    boolean replaceExisting = filters.contains("replace-existing:true");
    InputStream in = null;
    try {
      OperationAttachment attachment = operationContext.getAttachment(false);
      in = attachment.getStream();

      ZipInputStream zin = new ZipInputStream(in);
      ZipEntry ze = null;
      while ((ze = zin.getNextEntry()) != null) {
        String path = ze.getName().substring("wiki/".length());
        path = path.substring(0, path.length() - ".xml".length());
        int index = path.indexOf("/");
        String param0 = path.substring(0, index);
        String param1 = path.substring(index + 1);
        createSpace(param1, operationContext);
        Wiki wiki = mowService.getModel().getWikiStore().getWikiContainer(wikiType).getWiki(param1, true);
        Session session = mowService.getSession().getJCRSession();
        String nodePath = wiki.getWikiHome().getJCRPageNode().getParent().getPath();
        if (wiki != null) {
          if (replaceExisting) {
            log.info("Overwrite existing wiki '" + path + "'.");
            wiki.getWikiHome().getJCRPageNode().remove();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int size = 1024;
            byte[] buffer = new byte[size];
            int len;
            while ((len = zin.read(buffer)) > -1 ) {
                baos.write(buffer, 0, len);
                size = 2 * size;
                buffer = new byte[size];
            }
            baos.flush();
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            
            session.importXML(nodePath, is, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            session.save();
            zin.closeEntry();
          } else {
            log.info("Ignore existing wiki'" + path + "'.");
            continue;
          }
        }
        
      }
      resultHandler.completed(NoResultModel.INSTANCE);
      zin.close();
    } catch (Throwable exception) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "Error while proceeding wiki import.", exception);
    }
    if (in == null) {
      throw new OperationException(OperationNames.IMPORT_RESOURCE, "No data stream available for wiki import.");
    }

  }
  
  private void createSpace(String spaceName, OperationContext operationContext) {
    if (spaceName.indexOf("/") > -1) {
      spaceName = spaceName.substring(spaceName.lastIndexOf("/") + 1);
    }
    if (WikiType.GROUP.equals(wikiType)) {
      SpaceService spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);
      UserACL userAcl = operationContext.getRuntimeContext().getRuntimeComponent(UserACL.class);
      Space space = spaceService.getSpaceByPrettyName(SpaceUtils.cleanString(spaceName));
      if (space == null) {
        log.info("Automatically create new space: '" + spaceName + "'.");
        space = new Space();
        space.setPrettyName(spaceName);
        space.setDisplayName(spaceName);
        space.setGroupId("/spaces/" + spaceName);
        space.setRegistration(Space.OPEN);
        space.setDescription(spaceName);
        space.setType(DefaultSpaceApplicationHandler.NAME);
        space.setVisibility(Space.PUBLIC);
        space.setPriority(Space.INTERMEDIATE_PRIORITY);
        spaceService.createSpace(space, userAcl.getSuperUser());
      }
    }
  }

}
