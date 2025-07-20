package com.victor.filestorageapi.service.folder;

import com.victor.filestorageapi.models.entities.UserFolder;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface UserFolderDeleteService {
    //Soft delete
    @Transactional
    UserFolder softDeleteFolder(UUID userId, UUID folderId);

    @Transactional
    void recursiveSoftDeleteFolder(UUID userId, UUID folderId);
}
