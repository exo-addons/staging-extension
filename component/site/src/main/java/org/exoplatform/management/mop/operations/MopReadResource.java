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

package org.exoplatform.management.mop.operations;

import org.exoplatform.management.common.AbstractOperationHandler;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Class MopReadResource.
 *
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class MopReadResource extends AbstractOperationHandler {
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(OperationContext operationContext, ResultHandler resultHandler) throws OperationException {
    Set<String> children = new LinkedHashSet<String>(3);
    children.add("portalsites");
    children.add("groupsites");
    children.add("usersites");

    resultHandler.completed(new ReadResourceModel("Available site types.", children));
  }
}
