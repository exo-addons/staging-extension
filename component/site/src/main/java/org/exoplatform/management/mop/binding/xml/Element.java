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

package org.exoplatform.management.mop.binding.xml;

import org.staxnav.EnumElement;

/**
 * The Enum Element.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public enum Element implements EnumElement<Element>
{
  
  /** The unknown. */
  // Navigation Elements
  UNKNOWN(null),
  
  /** The node navigation. */
  NODE_NAVIGATION("node-navigation"),
  
  /** The priority. */
  PRIORITY("priority"),
  
  /** The page nodes. */
  PAGE_NODES("page-nodes"),
  
  /** The node. */
  NODE("node"),
  
  /** The uri. */
  @Deprecated
  URI("uri"),
  
  /** The parent uri. */
  PARENT_URI("parent-uri"),
  
  /** The label. */
  LABEL("label"),
  
  /** The start publication date. */
  START_PUBLICATION_DATE("start-publication-date"),
  
  /** The end publication date. */
  END_PUBLICATION_DATE("end-publication-date"),
  
  /** The visibility. */
  VISIBILITY("visibility"),
  
  /** The page reference. */
  PAGE_REFERENCE("page-reference"),

  /** The page set. */
  // Page elements
  PAGE_SET("page-set"),
  
  /** The page. */
  PAGE("page"),
  
  /** The name. */
  NAME("name"),
  
  /** The show max window. */
  SHOW_MAX_WINDOW("show-max-window"),

  /** The portal config. */
  // Portal config elements
  PORTAL_CONFIG("portal-config"),
  
  /** The portal name. */
  PORTAL_NAME("portal-name"),
  
  /** The locale. */
  LOCALE("locale"),
  
  /** The skin. */
  SKIN("skin"),
  
  /** The properties. */
  PROPERTIES("properties"),
  
  /** The properties entry. */
  PROPERTIES_ENTRY("entry"),
  
  /** The portal layout. */
  PORTAL_LAYOUT("portal-layout"),

  /** The title. */
  // Common elements
  TITLE("title"),
  
  /** The description. */
  DESCRIPTION("description"),
  
  /** The factory id. */
  FACTORY_ID("factory-id"),
  
  /** The access permissions. */
  ACCESS_PERMISSIONS("access-permissions"),
  
  /** The edit permission. */
  EDIT_PERMISSION("edit-permission"),
  
  /** The portlet application. */
  PORTLET_APPLICATION("portlet-application"),
  
  /** The gadget application. */
  GADGET_APPLICATION("gadget-application"),
  
  /** The container. */
  CONTAINER("container"),
  
  /** The page body. */
  PAGE_BODY("page-body"),
  
  /** The application ref. */
  APPLICATION_REF("application-ref"),
  
  /** The portlet ref. */
  PORTLET_REF("portlet-ref"),
  
  /** The portlet. */
  PORTLET("portlet"),
  
  /** The gadget ref. */
  GADGET_REF("gadget-ref"),
  
  /** The gadget. */
  GADGET("gadget"),
  
  /** The wsrp. */
  WSRP("wsrp"),
  
  /** The theme. */
  THEME("theme"),
  
  /** The show info bar. */
  SHOW_INFO_BAR("show-info-bar"),
  
  /** The show application state. */
  SHOW_APPLICATION_STATE("show-application-state"),
  
  /** The show application mode. */
  SHOW_APPLICATION_MODE("show-application-mode"),
  
  /** The icon. */
  ICON("icon"),
  
  /** The width. */
  WIDTH("width"),
  
  /** The height. */
  HEIGHT("height"),
  
  /** The preferences. */
  PREFERENCES("preferences"),
  
  /** The preference. */
  PREFERENCE("preference"),
  
  /** The preference value. */
  PREFERENCE_VALUE("value"),
  
  /** The preference readonly. */
  PREFERENCE_READONLY("read-only");

  /** The name. */
  private final String name;

  /**
   * Instantiates a new element.
   *
   * @param name the name
   */
  Element(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLocalName() {
    return name;
  }
}
