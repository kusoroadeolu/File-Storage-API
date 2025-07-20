package com.victor.filestorageapi.service.snapshots.impl;

import com.victor.filestorageapi.exception.UserFolderSnapshotCreationException;
import com.victor.filestorageapi.models.entities.UserFolder;
import com.victor.filestorageapi.models.entities.UserFolderSnapshot;
import com.victor.filestorageapi.models.entities.UserFolderSnapshotFileEntry;
import com.victor.filestorageapi.repository.UserFolderSnapshotFileEntryRepository;
import com.victor.filestorageapi.repository.UserFolderSnapshotRepository;
import com.victor.filestorageapi.service.aws.ObjectStorageService;
import com.victor.filestorageapi.service.snapshots.UserFolderSnapshotFileEntryService;
import com.victor.filestorageapi.service.snapshots.UserFolderSnapShotService;
import com.victor.filestorageapi.service.utils.FolderSnapshotUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFolderSnapshotServiceImpl implements UserFolderSnapShotService {

    private final UserFolderSnapshotRepository snapshotRepository;
    private final UserFolderSnapshotFileEntryRepository fileEntryRepository;
    private final FolderSnapshotUtils folderSnapshotUtils;
    private final UserFolderSnapshotFileEntryService folderSnapshotFileEntryService;
    private final ObjectStorageService objectStorageService;

    @Override
    @Transactional
    public UserFolderSnapshot createFolderSnapshot(UserFolder userFolder) {
        LocalDateTime timestamp = LocalDateTime.now();
        String folderPath = userFolder.getFolderPath();

        List<S3Object> allKeysUnderPrefix = objectStorageService.listS3ObjectsByPrefix(folderPath);
        List<UserFolderSnapshotFileEntry> fileEntriesToSave = null;

            try {
                fileEntriesToSave = allKeysUnderPrefix.stream()
                        .map(object -> folderSnapshotFileEntryService.buildFileEntry(object, new UserFolderSnapshot()))
                        .toList();
            } catch (Exception e) {
                log.error("An error occurred while trying to build file entry snapshots for folder snapshot: {}", folderPath);
                throw new UserFolderSnapshotCreationException(String.format("An error occurred while trying to build file entry snapshots for folder snapshot: %s", folderPath));
            }


        log.info("Attempting to build folder snapshot for: {}", folderPath);
        UserFolderSnapshot folderSnapshot = UserFolderSnapshot
                .builder()
                .userFolderId(userFolder.getId())
                .folderPath(folderPath)
                .folderSnapShotVersionId(folderSnapshotUtils.buildSnapshotVersion(timestamp))
                .snapshotTimestamp(timestamp)
                .fileEntries(fileEntriesToSave)
                .build();

        fileEntriesToSave.forEach(fileEntry -> fileEntry.setUserFolderSnapshot(folderSnapshot));


        try {
             snapshotRepository.save(folderSnapshot);
            log.info("Successfully built folder snapshot for: {}", folderPath);
            return folderSnapshot;
        } catch (DataIntegrityViolationException e) {
            log.error("Failed to save transient folder snapshot: {}", folderSnapshot.getFolderSnapShotVersionId());
            throw new UserFolderSnapshotCreationException(String.format("Failed to save transient folder snapshot: %s", folderSnapshot.getFolderSnapShotVersionId()), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred while trying to save transient folder snapshot: {}", folderSnapshot.getFolderSnapShotVersionId());
            throw new UserFolderSnapshotCreationException(String.format("Failed to save transient folder snapshot: %s", folderSnapshot.getFolderSnapShotVersionId()), e);
        }

    }
}
