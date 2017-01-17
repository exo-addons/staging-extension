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

import org.exoplatform.portal.pom.config.Utils;
import org.gatein.common.xml.stax.writer.WritableValueType;
import org.staxnav.StaxNavException;
import org.staxnav.ValueType;

/**
 * The Class DelimitedValueType.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class DelimitedValueType extends ValueType<String[]> implements WritableValueType<String[]> {
  
  /** The semi colon. */
  public static DelimitedValueType SEMI_COLON = new DelimitedValueType(";");

  /** The delimiter. */
  private final String delimiter;

  /**
   * Instantiates a new delimited value type.
   *
   * @param delimiter the delimiter
   */
  public DelimitedValueType(String delimiter) {
    this.delimiter = delimiter;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String[] parse(String s) throws Exception {
    return Utils.split(delimiter, s);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String format(String[] value) throws StaxNavException {
    String s = Utils.join(delimiter, value);

    if (s != null && s.trim().length() == 0) {
      return null;
    } else {
      return s;
    }
  }
}
