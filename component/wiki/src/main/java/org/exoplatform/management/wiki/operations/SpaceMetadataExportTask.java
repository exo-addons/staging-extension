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

import org.exoplatform.management.common.AbstractSpaceMetadataExportTask;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.wiki.mow.api.WikiType;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class SpaceMetadataExportTask extends AbstractSpaceMetadataExportTask {

  private final String wikiName;

  public SpaceMetadataExportTask(Space space, String wikiName) {
    super(space);
    this.wikiName = wikiName;
  }

  @Override
  public String getEntry() {
    return getEntryPath(WikiType.GROUP, wikiName);
  }

  public static String getEntryPath(WikiType wikiType, String wikiName) {
    return new StringBuilder("wiki/").append(wikiType.toString().toLowerCase()).append("/___").append(wikiName).append("---/").append(FILENAME).toString();
  }
}
