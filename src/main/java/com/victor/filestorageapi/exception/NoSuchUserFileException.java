package com.victor.filestorageapi.exception;

public class NoSuchUserFileException extends RuntimeException {
  public NoSuchUserFileException(String message) {
    super(message);
  }

  public NoSuchUserFileException(String message, Throwable cause) {
    super(message, cause);
  }
}
