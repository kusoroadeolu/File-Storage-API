package com.victor.filestorageapi.service.folder;

import jakarta.transaction.Transactional;

import java.util.UUID;

public interface UserFolderMoveService {
    @Transactional
    void moveFolder(UUID userId, UUID folderId, UUID newParentFolderId);
}
