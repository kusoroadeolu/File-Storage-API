package com.victor.filestorageapi.service.impl;

import com.victor.filestorageapi.dto.LoginRequestDto;
import com.victor.filestorageapi.dto.LoginResponseDto;
import com.victor.filestorageapi.dto.RegisterRequestDto;
import com.victor.filestorageapi.dto.RegisterResponseDto;
import com.victor.filestorageapi.exception.EmailAlreadyExistsException;
import com.victor.filestorageapi.exception.UsernameAlreadyExistsException;
import com.victor.filestorageapi.mapper.RequestMapper;
import com.victor.filestorageapi.models.Role;
import com.victor.filestorageapi.models.User;
import com.victor.filestorageapi.models.UserPrincipal;
import com.victor.filestorageapi.repository.UserRepository;
import com.victor.filestorageapi.service.AuthService;
import com.victor.filestorageapi.service.JwtService;
import com.victor.filestorageapi.service.MyUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final RequestMapper requestMapper;
    private final MyUserDetailsService myUserDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public LoginResponseDto authenticateUser(LoginRequestDto loginRequest) {
        UserPrincipal userPrincipal = (UserPrincipal) myUserDetailsService
                .loadUserByUsername(loginRequest.username());
        return requestMapper.mapLoginRequestToResponse(loginRequest, jwtService.generateToken(userPrincipal));
    }

    @Override
    public RegisterResponseDto registerUser(RegisterRequestDto registerRequest) {
        String email = registerRequest.email();
        String username = registerRequest.username();

        if(userRepository.existsByEmail(email)){
            throw new EmailAlreadyExistsException(String.format("Email '%s' is already taken", email));
        }
        if(userRepository.existsByUsername(username)){
            throw new UsernameAlreadyExistsException(String.format("Username '%s' is already taken", username));
        }


        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(registerRequest.password()))
                .role(Role.USER)
                .build();

        User saved = userRepository.save(user);
        UserPrincipal userPrincipal = (UserPrincipal) myUserDetailsService
                .loadUserByUsername(saved.getUsername());

        return requestMapper.mapRegisterRequestToResponse(registerRequest, jwtService.generateToken(userPrincipal));
    }
}
