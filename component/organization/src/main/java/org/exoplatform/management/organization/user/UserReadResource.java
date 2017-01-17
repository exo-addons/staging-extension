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
package org.exoplatform.management.organization.user;

import java.util.HashSet;
import java.util.Set;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.management.common.AbstractOperationHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

/**
 * The Class UserReadResource.
 *
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * @version $Revision$
 */
public class UserReadResource extends AbstractOperationHandler {
  
  /** The Constant log. */
  final private static Logger log = LoggerFactory.getLogger(UserReadResource.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Set<String> userNames = new HashSet<String>();

    OrganizationService organizationService = operationContext.getRuntimeContext().getRuntimeComponent(OrganizationService.class);
    try {
      ListAccess<User> allUsers = organizationService.getUserHandler().findAllUsers();
      int size = allUsers.getSize();
      int pageSize = 10;
      int i = 0;
      while (i < size) {
        int length = (size - i >= pageSize) ? pageSize : size - i;
        User[] users = allUsers.load(i, length);
        for (User user : users) {
          userNames.add(user.getUserName());
        }
        i += pageSize;
      }
      resultHandler.completed(new ReadResourceModel("List of all users.", userNames));
    } catch (Exception e) {
      log.error("Error occured while reading list of users.", e);
    }
  }
}