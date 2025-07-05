package com.victor.filestorageapi.mapper;

import com.victor.filestorageapi.dto.LoginRequestDto;
import com.victor.filestorageapi.dto.LoginResponseDto;
import com.victor.filestorageapi.dto.RegisterRequestDto;
import com.victor.filestorageapi.dto.RegisterResponseDto;
import org.springframework.stereotype.Service;

@Service
public class RequestMapper {
    public RegisterResponseDto mapRegisterRequestToResponse(RegisterRequestDto requestDto, String token){
        return new RegisterResponseDto(requestDto.username(), token);
    }

    public LoginResponseDto mapLoginRequestToResponse(LoginRequestDto requestDto, String token){
        return new LoginResponseDto(requestDto.username(), token);
    }
}
