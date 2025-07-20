package com.victor.filestorageapi.service.auth;

import com.victor.filestorageapi.models.dtos.RegisterRequestDto;
import com.victor.filestorageapi.models.dtos.RegisterResponseDto;

public interface AuthCommandService {
    public RegisterResponseDto registerUser(RegisterRequestDto registerRequest);
}
