package com.victor.filestorageapi.exception;

public class NoSuchUserFolderException extends RuntimeException {
    public NoSuchUserFolderException(String message) {
        super(message);
    }

    public NoSuchUserFolderException(String message, Throwable cause) {
        super(message, cause);
    }
}
