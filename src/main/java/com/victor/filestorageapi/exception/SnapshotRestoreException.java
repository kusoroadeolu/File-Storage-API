package com.victor.filestorageapi.exception;

public class SnapshotRestoreException extends RuntimeException {
    public SnapshotRestoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public SnapshotRestoreException(String message) {
        super(message);
    }
}
