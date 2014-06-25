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
package org.exoplatform.management.social.operations;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.exoplatform.management.social.SocialExtension;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.Dashboard;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.pom.spi.portlet.PortletState;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.container.UIContainer;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SocialDashboardExportTask implements ExportTask {

  public static final String FILENAME = "dashboard.metadata";

  private final UIContainer uiContainer;
  private final String spacePrettyName;

  public SocialDashboardExportTask(UIContainer uiContainer, String spacePrettyName) {
    this.spacePrettyName = spacePrettyName;
    this.uiContainer = uiContainer;
  }

  @Override
  public String getEntry() {
    return new StringBuilder("social/space/").append(spacePrettyName).append("/").append(FILENAME).toString();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    XStream xStream = new XStream();
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
    xStream.autodetectAnnotations(false);
    xStream.setMode(XStream.NO_REFERENCES);
    xStream.omitField(UIPortlet.class, "supportedProcessingEvents");
    xStream.omitField(UIPortlet.class, "producedOfferedPortlet");
    xStream.omitField(UIPortlet.class, "supportedPublicRenderParameters");
    xStream.omitField(UIPortlet.class, "portletContext");
    xStream.omitField(UIPortlet.class, "componentConfig");
    xStream.omitField(PortletState.class, "applicationState");

    xStream.toXML(uiContainer, writer);
    writer.flush();
  }

  public static Dashboard getDashboard(DataStorage dataStorage, String spaceGroupId) throws Exception {
    dataStorage.save();
    Page page = dataStorage.getPage("group::" + spaceGroupId + "::" + SocialExtension.DASHBOARD_PORTLET);
    if (page == null) {
      return null;
    }
    ArrayList<ModelObject> modelObjects = page.getChildren();
    return getDashboard(dataStorage, modelObjects);
  }

  private static Dashboard getDashboard(DataStorage dataStorage, ArrayList<ModelObject> children) throws Exception {
    if (children != null) {
      for (ModelObject modelObject : children) {
        if (modelObject instanceof Container) {
          try {
            Dashboard dashboard = dataStorage.loadDashboard(modelObject.getStorageId());
            if (dashboard != null) {
              return dashboard;
            }
          } catch (Exception e) {
            // Not dashboard
          }
        }
      }
    }
    return null;
  }

}
