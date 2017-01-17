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

import org.exoplatform.management.mop.operations.navigation.NavigationKey;
import org.exoplatform.management.mop.operations.navigation.NavigationUtils;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.gatein.management.api.binding.Marshaller;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The Class NavigationExportTask.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class NavigationExportTask extends AbstractExportTask {
  
  /** The Constant FILE. */
  public static final String FILE = "navigation.xml";

  /** The navigation key. */
  private NavigationKey navigationKey;
  
  /** The marshaller. */
  private Marshaller<PageNavigation> marshaller;
  
  /** The navigation service. */
  private NavigationService navigationService;
  
  /** The description service. */
  private DescriptionService descriptionService;

  /**
   * Instantiates a new navigation export task.
   *
   * @param navigationKey the navigation key
   * @param navigationService the navigation service
   * @param descriptionService the description service
   * @param marshaller the marshaller
   */
  public NavigationExportTask(NavigationKey navigationKey, NavigationService navigationService, DescriptionService descriptionService, Marshaller<PageNavigation> marshaller) {
    super(navigationKey.getSiteKey());
    this.navigationKey = navigationKey;
    this.navigationService = navigationService;
    this.descriptionService = descriptionService;
    this.marshaller = marshaller;
  }

  /** The navigation. */
  // TODO: This is a little sloppy to support filtering, fix if we have time.
  private PageNavigation navigation;

  /**
   * Instantiates a new navigation export task.
   *
   * @param navigation the navigation
   * @param marshaller the marshaller
   */
  public NavigationExportTask(PageNavigation navigation, Marshaller<PageNavigation> marshaller) {
    super(new SiteKey(navigation.getOwnerType(), navigation.getOwnerId()));
    this.navigation = navigation;
    this.marshaller = marshaller;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getXmlFileName() {
    return FILE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(OutputStream outputStream) throws IOException {
    if (navigation == null) {
      navigation = NavigationUtils.loadPageNavigation(navigationKey, navigationService, descriptionService);
    }

    marshaller.marshal(navigation, outputStream, false);
  }
}
