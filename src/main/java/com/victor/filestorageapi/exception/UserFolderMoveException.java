package com.victor.filestorageapi.exception;

public class UserFolderMoveException extends RuntimeException {
    public UserFolderMoveException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserFolderMoveException(String message) {
        super(message);
    }
}
