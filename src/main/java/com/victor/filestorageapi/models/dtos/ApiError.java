package com.victor.filestorageapi.models.dtos;


import java.time.LocalDateTime;

public record ApiError(int status, String message, LocalDateTime thrownAt) {
}
