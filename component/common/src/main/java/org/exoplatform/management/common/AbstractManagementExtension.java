package org.exoplatform.management.common;

import java.util.Arrays;
import java.util.HashSet;

import org.gatein.management.api.ManagedDescription;
import org.gatein.management.api.exceptions.OperationException;
import org.gatein.management.api.exceptions.ResourceNotFoundException;
import org.gatein.management.api.operation.OperationContext;
import org.gatein.management.api.operation.ResultHandler;
import org.gatein.management.api.operation.model.ReadResourceModel;
import org.gatein.management.spi.ManagementExtension;

public abstract class AbstractManagementExtension implements ManagementExtension {

  @Override
  public void destroy() {}

  protected static ManagedDescription description(final String description) {
    return new ManagedDescription() {
      @Override
      public String getDescription() {
        return description;
      }
    };
  }

  public static class EmptyReadResource extends AbstractOperationHandler {
    @Override
    public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
      resultHandler.completed(new ReadResourceModel("Empty", new HashSet<String>()));
    }
  }

  public static class ReadResource extends AbstractOperationHandler {
    private String[] values;
    private String description;

    public ReadResource(String description, String... values) {
      this.values = values;
      this.description = description;
    }

    @Override
    public void execute(OperationContext operationContext, ResultHandler resultHandler) throws ResourceNotFoundException, OperationException {
      resultHandler.completed(new ReadResourceModel(description, values == null ? new HashSet<String>() : new HashSet<String>(Arrays.asList(values))));
    }

  }
}
