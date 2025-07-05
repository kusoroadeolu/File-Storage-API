package com.victor.filestorageapi.controlleradvice;

import com.victor.filestorageapi.dto.ApiError;
import com.victor.filestorageapi.exception.EmailAlreadyExistsException;
import com.victor.filestorageapi.exception.JwtAuthenticationException;
import com.victor.filestorageapi.exception.UsernameAlreadyExistsException;
import com.victor.filestorageapi.exception.UsernameNotFoundException;
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

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFoundException(UsernameNotFoundException ex){
        ApiError apiError = new ApiError(404, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(404).body(apiError);
    }
}
