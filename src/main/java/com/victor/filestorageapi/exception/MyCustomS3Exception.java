package com.victor.filestorageapi.exception;

public class MyCustomS3Exception extends RuntimeException {
    public MyCustomS3Exception(String message) {
        super(message);
    }

    public MyCustomS3Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
