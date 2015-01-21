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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.management.answer.AnswerExtension;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * @author <a href="mailto:bkhanfir@exoplatform.com">Boubaker Khanfir</a>
 * @version $Revision$
 */
public class AnswerDataReadResource extends AbstractOperationHandler {

  private boolean isSpaceType;

  final private static Logger log = LoggerFactory.getLogger(AnswerDataReadResource.class);

  public AnswerDataReadResource(boolean isSpaceType) {
    this.isSpaceType = isSpaceType;
  }

  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
    Set<String> children = new LinkedHashSet<String>();
    FAQService faqService = operationContext.getRuntimeContext().getRuntimeComponent(FAQService.class);
    SpaceService spaceService = operationContext.getRuntimeContext().getRuntimeComponent(SpaceService.class);

    try {
      List<Category> categories = faqService.getAllCategories();
      for (Category category : categories) {
        if ((isSpaceType && !category.getId().startsWith(Utils.CATE_SPACE_ID_PREFIX)) || (!isSpaceType && category.getId().startsWith(Utils.CATE_SPACE_ID_PREFIX))) {
          continue;
        }
        String name = category.getName();
        if (isSpaceType) {
          String spaceGroupId = SpaceUtils.SPACE_GROUP + "/" + category.getId().replace(Utils.CATE_SPACE_ID_PREFIX, "");
          Space space = spaceService.getSpaceByGroupId(spaceGroupId);
          if (space == null) {
            continue;
          }
          name = space.getDisplayName();
        }
        children.add(name);
      }
      if (!isSpaceType) {
        children.add(AnswerExtension.ROOT_CATEGORY);
      }
    } catch (Exception e) {
      log.error("Error while listing FAQ categories.", e);
    }
    resultHandler.completed(new ReadResourceModel("All FAQ:", children));
  }
}
