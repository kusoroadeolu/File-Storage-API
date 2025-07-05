package com.victor.filestorageapi.dto;


import java.time.LocalDateTime;

public record ApiError(int status, String message, LocalDateTime thrownAt) {
}
