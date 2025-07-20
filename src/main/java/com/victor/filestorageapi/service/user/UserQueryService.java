package com.victor.filestorageapi.service.user;

import com.victor.filestorageapi.models.entities.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface UserQueryService {
    User findUserById(UUID userId);
}
