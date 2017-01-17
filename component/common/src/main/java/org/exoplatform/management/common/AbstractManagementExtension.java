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
package org.exoplatform.management.common;

import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.management.spi.ManagementExtension;

import java.util.Arrays;
import java.util.HashSet;

/**
 * The Class AbstractManagementExtension.
 */
public abstract class AbstractManagementExtension implements ManagementExtension {

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {}

  /**
   * Description.
   *
   * @param description the description
   * @return the managed description
   */
  protected static ManagedDescription description(final String description) {
    return new ManagedDescription() {
      @Override
      public String getDescription() {
        return description;
      }
    };
  }

  /**
   * The Class EmptyReadResource.
   */
  public static class EmptyReadResource extends AbstractOperationHandler {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
      resultHandler.completed(new ReadResourceModel("Empty", new HashSet<String>()));
    }
  }

  /**
   * The Class ReadResource.
   */
  public static class ReadResource extends AbstractOperationHandler {
    
    /** The values. */
    private String[] values;
    
    /** The description. */
    private String description;

    /**
     * Instantiates a new read resource.
     *
     * @param description the description
     * @param values the values
     */
    public ReadResource(String description, String... values) {
      this.values = values;
      this.description = description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
      resultHandler.completed(new ReadResourceModel(description, values == null ? new HashSet<String>() : new HashSet<String>(Arrays.asList(values))));
    }

  }
}
