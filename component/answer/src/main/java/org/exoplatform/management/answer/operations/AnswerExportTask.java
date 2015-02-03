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
import java.io.OutputStreamWriter;
import java.util.List;

import org.chromattic.common.collection.Collections;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.Question;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class AnswerExportTask implements ExportTask {
  public static final String FILENAME = "/category.xml";

  private final String type;
  private final Category category;
  private final List<Question> questions;

  public AnswerExportTask(String type, Category category, List<Question> questions) {
    this.category = category;
    this.type = type;
    this.questions = questions;
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
    XStream xStream = new XStream();
    OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");

    for (Question question : questions) {
      for (Answer answer : question.getAnswers()) {
        if (answer.getLanguage() == null) {
          answer.setLanguage(question.getLanguage());
        }
      }
    }

    List<Object> objects = Collections.list(category, questions);
    xStream.toXML(objects, writer);
    writer.flush();
  }
}
