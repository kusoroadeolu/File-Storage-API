package com.victor.filestorageapi.service.snapshots;

import com.victor.filestorageapi.models.entities.UserFolderSnapshot;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.UUID;

public interface UserFolderSnapshotRestoreService {
    @Transactional
    void restoreFolderSnapshot(UUID userId, UUID snapshotId);

    void rollBackSnapshotRestore(UserFolderSnapshot snapshot, Map<String, String> objectsToRollback, Map<String, String> mapRestoredObjectKeysToVersionId);
}
