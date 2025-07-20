package com.victor.filestorageapi.exception;

public class UserFolderSnapshotCreationException extends RuntimeException {

  public UserFolderSnapshotCreationException(String message, Throwable cause) {
    super(message, cause);
  }

  public UserFolderSnapshotCreationException(String message) {
        super(message);
    }
}
