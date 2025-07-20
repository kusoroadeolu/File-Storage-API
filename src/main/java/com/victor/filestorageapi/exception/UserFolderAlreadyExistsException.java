package com.victor.filestorageapi.exception;

public class UserFolderAlreadyExistsException extends RuntimeException {

    private Exception exception;

    public UserFolderAlreadyExistsException(String message) {
        super(message);
    }

    public UserFolderAlreadyExistsException(String message, Exception exception) {
        super(message);
        this.exception = exception;
    }
}
