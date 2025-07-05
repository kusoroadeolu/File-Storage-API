package com.victor.filestorageapi.service;

import com.victor.filestorageapi.dto.LoginRequestDto;
import com.victor.filestorageapi.dto.LoginResponseDto;
import com.victor.filestorageapi.dto.RegisterRequestDto;
import com.victor.filestorageapi.dto.RegisterResponseDto;

public interface AuthService {
    public LoginResponseDto authenticateUser(LoginRequestDto loginRequest);
    public RegisterResponseDto registerUser(RegisterRequestDto registerRequest);
}
