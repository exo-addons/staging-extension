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
package org.exoplatform.management.calendar.operations;

import org.exoplatform.management.common.AbstractSpaceMetadataExportTask;
import org.exoplatform.social.core.space.model.Space;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SpaceMetadataExportTask extends AbstractSpaceMetadataExportTask {

  // In case the space is renamed, the space pretty name still the old one
  private final String spacePrettyName;

  public SpaceMetadataExportTask(Space space, String spacePrettyName) {
    super(space);
    this.spacePrettyName = spacePrettyName;
  }

  @Override
  public String getEntry() {
    return getEntryPath(spacePrettyName);
  }

  public static String getEntryPath(String spacePrettyName) {
    return new StringBuilder("calendar/space/").append(spacePrettyName).append("/").append(FILENAME).toString();
  }
}
