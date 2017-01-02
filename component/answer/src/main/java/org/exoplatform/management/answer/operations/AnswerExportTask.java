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
package org.exoplatform.management.answer.operations;

import java.io.IOException;
import java.io.OutputStream;
import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.faq.service.Category;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.exoplatform.faq.service.FAQService;
import org.gatein.management.api.operation.model.ExportTask;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class AnswerExportTask implements ExportTask {
  public static final String FILENAME = "/category.xml";
  final private static Logger log      = LoggerFactory.getLogger(AnswerDataImportResource.class);

  private final String type;
  private final Category category;

  public AnswerExportTask(String type, Category category) {
    this.category = category;
    this.type = type;
  }

  @Override
  public String getEntry() {
    return getEntryPath(type, category.getId());
  }

  public static String getEntryPath(String type, String id) {
    return new StringBuilder("answer/").append(type).append("/").append(id).append(FILENAME).toString();
  }

  @Override
  public void export(OutputStream outputStream) throws IOException {
    FAQService faqService = (FAQService) PortalContainer.getInstance().getComponentInstanceOfType(FAQService.class);
    try {
      Node categoryNode = faqService.getCategoryNodeById(category.getId());
      Session session = categoryNode.getSession();
      session.exportSystemView(categoryNode.getPath(), outputStream, false, false);
    } catch (Exception e) {
      log.error("Fail to export data:", e);
    }
  }
}
