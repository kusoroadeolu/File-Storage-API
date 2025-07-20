package com.victor.filestorageapi.exception;

public class NoSuchSnapshotException extends RuntimeException {

    public NoSuchSnapshotException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchSnapshotException(String message) {
        super(message);
    }
}
