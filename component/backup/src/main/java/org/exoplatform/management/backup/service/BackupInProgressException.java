package org.exoplatform.management.backup.service;

public class BackupInProgressException extends RuntimeException {

  private static final long serialVersionUID = 6455071395384858777L;

  public BackupInProgressException() {
    super("Backup or a restore is in progress");
  }

  public BackupInProgressException(String message) {
    super(message);
  }

}
