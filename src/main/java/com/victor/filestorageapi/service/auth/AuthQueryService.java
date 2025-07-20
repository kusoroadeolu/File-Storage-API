package com.victor.filestorageapi.service.auth;

import com.victor.filestorageapi.models.dtos.LoginRequestDto;
import com.victor.filestorageapi.models.dtos.LoginResponseDto;
import org.springframework.stereotype.Service;

@Service
public interface AuthQueryService {
    public LoginResponseDto authenticateUser(LoginRequestDto loginRequest);
}
