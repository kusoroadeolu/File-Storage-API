package com.victor.filestorageapi.service.auth.impl;

import com.victor.filestorageapi.models.dtos.LoginRequestDto;
import com.victor.filestorageapi.models.dtos.LoginResponseDto;
import com.victor.filestorageapi.exception.InvalidPasswordException;
import com.victor.filestorageapi.mapper.AuthRequestMapper;
import com.victor.filestorageapi.models.entities.UserPrincipal;
import com.victor.filestorageapi.repository.UserRepository;
import com.victor.filestorageapi.service.auth.AuthQueryService;
import com.victor.filestorageapi.service.auth.JwtService;
import com.victor.filestorageapi.service.auth.MyUserDetailsQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthQueryServiceImpl implements AuthQueryService {
    private final AuthRequestMapper requestMapper;
    private final MyUserDetailsQueryService myUserDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public LoginResponseDto authenticateUser(LoginRequestDto loginRequest) {
        String username = loginRequest.username().trim();
        String password = loginRequest.password().trim();

        UserPrincipal userPrincipal = (UserPrincipal) myUserDetailsService
                .loadUserByUsername(username);
        log.info("Attempting to authenticate user with username: {}", username);

        if(!passwordEncoder.matches(password, userPrincipal.getPassword())){
            throw new InvalidPasswordException("Invalid Password");
        }

        log.info("Successfully authenticated user: {}", username);

        return requestMapper.mapLoginRequestToResponse(loginRequest, jwtService.generateToken(userPrincipal));
    }
}
