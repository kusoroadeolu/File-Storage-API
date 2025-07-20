package com.victor.filestorageapi.exception;

public class UserFolderCreationException extends RuntimeException {

    public UserFolderCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserFolderCreationException(String message) {
        super(message);
    }
}
