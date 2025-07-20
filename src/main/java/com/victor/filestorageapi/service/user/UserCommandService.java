package com.victor.filestorageapi.service.user;

import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.models.entities.UserFolder;
import org.springframework.stereotype.Service;

@Service
public interface UserCommandService {
    void assignRootFolder(User user, UserFolder userRootFolder);
}
