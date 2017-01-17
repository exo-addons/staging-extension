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

package org.exoplatform.management.mop.exportimport;

import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.gatein.management.api.binding.Marshaller;
import org.gatein.management.api.operation.model.ExportTask;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * The Class SiteLayoutExportTask.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class SiteLayoutExportTask extends AbstractExportTask implements ExportTask {
  
  /** The Constant FILES. */
  public static final Set<String> FILES;
  static {
    HashSet<String> tmp = new HashSet<String>(3);
    tmp.add("portal.xml");
    tmp.add("group.xml");
    tmp.add("user.xml");
    FILES = tmp;
  }

  /** The data storage. */
  private final DataStorage dataStorage;
  
  /** The marshaller. */
  private final Marshaller<PortalConfig> marshaller;

  /**
   * Instantiates a new site layout export task.
   *
   * @param siteKey the site key
   * @param dataStorage the data storage
   * @param marshaller the marshaller
   */
  public SiteLayoutExportTask(SiteKey siteKey, DataStorage dataStorage, Marshaller<PortalConfig> marshaller) {
    super(siteKey);
    this.dataStorage = dataStorage;
    this.marshaller = marshaller;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getXmlFileName() {
    if (siteKey.getType() == SiteType.PORTAL) {
      return "portal.xml";
    } else if (siteKey.getType() == SiteType.GROUP) {
      return "group.xml";
    } else if (siteKey.getType() == SiteType.USER) {
      return "user.xml";
    } else {
      throw new RuntimeException("Unknown site type " + siteKey.getType());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    PortalConfig portalConfig;
    try {
      portalConfig = dataStorage.getPortalConfig(siteKey.getTypeName(), siteKey.getName());
    } catch (Exception e) {
      throw new IOException("Could not retrieve site " + siteKey, e);
    }

    marshaller.marshal(portalConfig, outputStream, false);
  }
}
