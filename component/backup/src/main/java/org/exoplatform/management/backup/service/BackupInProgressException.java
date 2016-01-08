package org.exoplatform.management.backup.service;

public class BackupInProgressException extends RuntimeException {
  public static final long serialVersionUID = 6455071395384858777L;

  public static ThreadLocal<Boolean> exceptionCaught = new ThreadLocal<Boolean>();

  public BackupInProgressException() {
    this("Backup or a restore is in progress");
  }

  public BackupInProgressException(String message) {
    super(message);
    exceptionCaught.set(true);
  }

}
