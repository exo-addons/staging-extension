package org.exoplatform.management.common;

public interface DataTransformerPlugin {
  public void exportData(Object... objects);

  public void importData(Object... objects);
}