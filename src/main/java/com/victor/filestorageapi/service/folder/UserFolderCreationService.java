package com.victor.filestorageapi.service.folder;

import com.victor.filestorageapi.models.entities.UserFolder;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface UserFolderCreationService {

    @Transactional
    UserFolder createRootFolder(UUID userId);

    @Transactional
    UserFolder createSubFolder(UUID userID, UUID parentFolderId, String folderName);
}
