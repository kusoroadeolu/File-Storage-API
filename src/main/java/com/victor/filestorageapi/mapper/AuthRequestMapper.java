package com.victor.filestorageapi.mapper;

import com.victor.filestorageapi.models.dtos.LoginRequestDto;
import com.victor.filestorageapi.models.dtos.LoginResponseDto;
import com.victor.filestorageapi.models.dtos.RegisterRequestDto;
import com.victor.filestorageapi.models.dtos.RegisterResponseDto;
import org.springframework.stereotype.Service;

@Service
public class AuthRequestMapper {
    public RegisterResponseDto mapRegisterRequestToResponse(RegisterRequestDto requestDto, String token){
        return new RegisterResponseDto(requestDto.username(), token);
    }

    public LoginResponseDto mapLoginRequestToResponse(LoginRequestDto requestDto, String token){
        return new LoginResponseDto(requestDto.username(), token);
    }
}
