package com.victor.filestorageapi.service.user.impl;

import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.models.entities.UserFolder;
import com.victor.filestorageapi.repository.UserRepository;
import com.victor.filestorageapi.service.user.UserCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;

    @Override
    public void assignRootFolder(User user, UserFolder userRootFolder) {
        user.setRootFolder(userRootFolder);
        userRepository.save(user);
    }
}
