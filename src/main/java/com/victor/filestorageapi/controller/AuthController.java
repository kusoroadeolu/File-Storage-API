package com.victor.filestorageapi.controller;

import com.victor.filestorageapi.dto.LoginRequestDto;
import com.victor.filestorageapi.dto.LoginResponseDto;
import com.victor.filestorageapi.dto.RegisterRequestDto;
import com.victor.filestorageapi.dto.RegisterResponseDto;
import com.victor.filestorageapi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<LoginResponseDto> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequest){
        LoginResponseDto responseDto = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok().body(responseDto);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RegisterResponseDto> registerUser(@Valid @RequestBody RegisterRequestDto registerRequest){
        RegisterResponseDto responseDto = authService.registerUser(registerRequest);
        return ResponseEntity.status(201).body(responseDto);
    }

}
