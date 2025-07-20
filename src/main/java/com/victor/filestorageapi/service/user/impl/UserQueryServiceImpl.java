package com.victor.filestorageapi.service.user.impl;

import com.victor.filestorageapi.exception.UserNotFoundException;
import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.repository.UserRepository;
import com.victor.filestorageapi.service.user.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    @Override
    public User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Could not find user with id: " + userId));
    }
}
