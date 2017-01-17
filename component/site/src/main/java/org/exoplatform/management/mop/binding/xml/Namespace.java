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

/**
 * The Enum Namespace.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public enum Namespace
{
  
  /** The gatein objects 1 1. */
  GATEIN_OBJECTS_1_1("http://www.gatein.org/xml/ns/gatein_objects_1_1"),
  
  /** The gatein objects 1 2. */
  GATEIN_OBJECTS_1_2("http://www.gatein.org/xml/ns/gatein_objects_1_2"),
  
  /** The gatein objects 1 3. */
  GATEIN_OBJECTS_1_3("http://www.gatein.org/xml/ns/gatein_objects_1_3");

  /**
   * The current namespace version.
   */
  public static final Namespace CURRENT = GATEIN_OBJECTS_1_3;

  /** The name. */
  private final String name;

  /**
   * Instantiates a new namespace.
   *
   * @param name the name
   */
  Namespace(final String name) {
    this.name = name;
  }

  /**
   * Get the URI of this namespace.
   *
   * @return the URI
   */
  public String getUri() {
    return name;
  }
}
