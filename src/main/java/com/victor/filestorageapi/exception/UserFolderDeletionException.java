package com.victor.filestorageapi.exception;

public class UserFolderDeletionException extends RuntimeException {
    public UserFolderDeletionException(String message) {
        super(message);
    }

    public UserFolderDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
