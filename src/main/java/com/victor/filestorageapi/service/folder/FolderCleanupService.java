package com.victor.filestorageapi.service.folder;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public interface FolderCleanupService {
    @Transactional
    public void cleanupDeletedFolders();
}
