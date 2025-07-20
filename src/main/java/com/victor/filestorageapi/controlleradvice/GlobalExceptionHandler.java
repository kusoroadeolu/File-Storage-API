package com.victor.filestorageapi.controlleradvice;

import com.victor.filestorageapi.models.dtos.ApiError;
import com.victor.filestorageapi.exception.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ApiError> handleJwtAuthenticationException(JwtAuthenticationException ex){
        ApiError apiError = new ApiError(401, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(401).body(apiError);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex){
        ApiError apiError = new ApiError(409, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(409).body(apiError);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex){
        ApiError apiError = new ApiError(409, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(409).body(apiError);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFoundException(UserNotFoundException ex){
        ApiError apiError = new ApiError(404, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(404).body(apiError);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiError> handleInvalidPasswordException(InvalidPasswordException ex){
        ApiError apiError = new ApiError(404, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(404).body(apiError);
    }

    @ExceptionHandler(UserFolderAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUserFolderAlreadyExistsException(UserFolderAlreadyExistsException ex){
        ApiError apiError = new ApiError(409, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(409).body(apiError);
    }

    @ExceptionHandler(NoSuchUserFolderException.class)
    public ResponseEntity<ApiError> handleNoSuchUserFolderException(NoSuchUserFolderException ex){
        ApiError apiError = new ApiError(404, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(404).body(apiError);
    }

    @ExceptionHandler(UserFolderCreationException.class)
    public ResponseEntity<ApiError> handleFolderCreationException(UserFolderCreationException ex){
        ApiError apiError = new ApiError(500, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(500).body(apiError);
    }

    @ExceptionHandler(UserFolderDeletionException.class)
    public ResponseEntity<ApiError> handleFolderDeletionException(UserFolderDeletionException ex){
        ApiError apiError = new ApiError(404, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(404).body(apiError);
    }

    @ExceptionHandler(UserFolderMoveException.class)
    public ResponseEntity<ApiError> handleUserFolderMoveException(UserFolderMoveException ex){
        ApiError apiError = new ApiError(500, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(500).body(apiError);
    }

    @ExceptionHandler(NoSuchUserFileException.class)
    public ResponseEntity<ApiError> handleNoSuchUserFileException(NoSuchUserFileException ex){
        ApiError apiError = new ApiError(404, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(404).body(apiError);
    }

    @ExceptionHandler(MyCustomS3Exception.class)
    public ResponseEntity<ApiError> handleCustomS3Exception(MyCustomS3Exception ex){
        ApiError apiError = new ApiError(500, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(500).body(apiError);
    }

    @ExceptionHandler(NoSuchSnapshotException.class)
    public ResponseEntity<ApiError> handleNoSuchSnapshotException(NoSuchSnapshotException ex){
        ApiError apiError = new ApiError(404, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(404).body(apiError);
    }

    @ExceptionHandler(SnapshotRestoreException.class)
    public ResponseEntity<ApiError> handleSnapshotRestoreException(SnapshotRestoreException ex){
        ApiError apiError = new ApiError(500, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(500).body(apiError);
    }
}
