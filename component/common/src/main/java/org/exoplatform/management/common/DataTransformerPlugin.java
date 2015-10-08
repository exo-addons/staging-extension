package org.exoplatform.management.common;

public interface DataTransformerPlugin {
  public Object exportData(Object... objects);
  public Object importData(Object... objects);
}