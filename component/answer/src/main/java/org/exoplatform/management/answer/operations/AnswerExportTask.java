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
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.util.IOUtils;
import org.chromattic.common.collection.Collections;
import org.exoplatform.faq.service.*;
import org.exoplatform.management.answer.AnswerExtension;
import org.exoplatform.management.common.InputStreamWrapper;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.operation.model.ExportTask;

import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class AnswerExportTask implements ExportTask {
  public static final String FILENAME = "/category.xml";
  final private static Logger log = LoggerFactory.getLogger(AnswerExportTask.class);

  private final String type;
  private final Category category;
  private final List<Question> questions;
  private final FAQService faqService;
  private final boolean exportSubCategories;

  public AnswerExportTask(String type, Category category, List<Question> questions, FAQService faqService, boolean exportSubCategories) {
    this.category = category;
    this.type = type;
    this.questions = questions;
    this.faqService = faqService;
    this.exportSubCategories = exportSubCategories;
  }

  @Override
  public String getEntry() {
    return getEntryPath(type, category.getId());
  }

  public static String getEntryPath(String type, String id) {
    return new StringBuilder("answer/").append(type).append("/").append(id).append(FILENAME).toString();
  }
  private List<Category> getAllSubCategories(String path) {
    List<Category> categories = new ArrayList<Category>();
    List<Category> listOfSubCategory = new ArrayList<Category>();
    String parentPath = "";
    try {
      categories = faqService.getAllCategories();
      for (Category category : categories) {
        parentPath = category.getPath().replace("/" + category.getId(), "");
        if (parentPath.equals(path)) {
          listOfSubCategory.add(category);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return listOfSubCategory;
  }
  private void addSubCategories(Category category,List<Object> objects ){
    try {
      List<Category> listOfSubCategory = getAllSubCategories(category.getPath());
      for(Category cat : listOfSubCategory) {
        if (!cat.getName().equals("categories")) {
          QuestionPageList subQuestionsPageList = faqService.getAllQuestionsByCatetory(cat.getId(), AnswerExtension.EMPTY_FAQ_SETTIGNS);
          List<Question> subQuestions = subQuestionsPageList.getAll();
          for (Question subQuestion : subQuestions) {
            if (subQuestion.getAttachMent() != null && !subQuestion.getAttachMent().isEmpty()) {
              List<FileAttachment> attachments = subQuestion.getAttachMent();
              for (FileAttachment fileAttachment : attachments) {
                InputStreamWrapper inputStream = new InputStreamWrapper(IOUtils.toByteArray(fileAttachment.getInputStream()));
                fileAttachment.setInputStream(inputStream);
              }
            }
          }
          for (Question subQuestion : subQuestions) {
            for (Answer answer : subQuestion.getAnswers()) {
              if (answer.getLanguage() == null) {
                answer.setLanguage(subQuestion.getLanguage());
              }
            }
          }
          objects.add(cat);
          objects.add(subQuestions);
        }
          addSubCategories(cat, objects);
      }
    }catch (Exception e){
      log.error("Error when exporting subCategories");
    }
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
    if(exportSubCategories) {
      addSubCategories(category, objects);
    }
    xStream.toXML(objects, writer);
    writer.flush();
  }
}
