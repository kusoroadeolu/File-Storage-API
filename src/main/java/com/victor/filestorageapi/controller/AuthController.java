package com.victor.filestorageapi.controller;

import com.victor.filestorageapi.models.dtos.LoginRequestDto;
import com.victor.filestorageapi.models.dtos.LoginResponseDto;
import com.victor.filestorageapi.models.dtos.RegisterRequestDto;
import com.victor.filestorageapi.models.dtos.RegisterResponseDto;
import com.victor.filestorageapi.service.auth.AuthCommandService;
import com.victor.filestorageapi.service.auth.AuthQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthCommandService authCommandService;
    private final AuthQueryService authQueryService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<LoginResponseDto> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequest){
        LoginResponseDto responseDto = authQueryService.authenticateUser(loginRequest);
        return ResponseEntity.ok().body(responseDto);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RegisterResponseDto> registerUser(@Valid @RequestBody RegisterRequestDto registerRequest){
        RegisterResponseDto responseDto = authCommandService.registerUser(registerRequest);
        return ResponseEntity.status(201).body(responseDto);
    }

}
