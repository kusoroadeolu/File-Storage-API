package com.victor.filestorageapi.service.auth.impl;

import com.victor.filestorageapi.models.dtos.RegisterRequestDto;
import com.victor.filestorageapi.models.dtos.RegisterResponseDto;
import com.victor.filestorageapi.exception.EmailAlreadyExistsException;
import com.victor.filestorageapi.exception.UsernameAlreadyExistsException;
import com.victor.filestorageapi.mapper.AuthRequestMapper;
import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.models.entities.UserPrincipal;
import com.victor.filestorageapi.models.enums.Role;
import com.victor.filestorageapi.repository.UserRepository;
import com.victor.filestorageapi.service.auth.AuthCommandService;
import com.victor.filestorageapi.service.auth.JwtService;
import com.victor.filestorageapi.service.auth.MyUserDetailsQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthCommandServiceImpl implements AuthCommandService {

    private final AuthRequestMapper requestMapper;
    private final MyUserDetailsQueryService myUserDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public RegisterResponseDto registerUser(RegisterRequestDto registerRequest) {
        String email = registerRequest.email().trim();
        String username = registerRequest.username().trim();

        log.info("Attempting to register user with username: {}", username);
        if(userRepository.existsByEmail(email)){
            throw new EmailAlreadyExistsException(String.format("Email '%s' is already taken", email));
        }

        if(userRepository.existsByUsername(username)){
            throw new UsernameAlreadyExistsException(String.format("Username '%s' is already taken", username));
        }


        User user = new User(
          username,
          email,
          passwordEncoder.encode(registerRequest.password()),
          Role.USER
        );

        User saved = userRepository.save(user);
        log.info("Saved user with username: {}", username);

        UserPrincipal userPrincipal = (UserPrincipal) myUserDetailsService
                .loadUserByUsername(saved.getUsername());
        log.info("Authenticated user with username: {}", username);


        return requestMapper.mapRegisterRequestToResponse(registerRequest, jwtService.generateToken(userPrincipal));
    }
}
