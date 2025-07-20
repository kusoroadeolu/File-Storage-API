package com.victor.filestorageapi.service.snapshots.impl;

import com.victor.filestorageapi.models.entities.UserFolderSnapshot;
import com.victor.filestorageapi.models.entities.UserFolderSnapshotFileEntry;
import com.victor.filestorageapi.service.aws.ObjectQueryService;
import com.victor.filestorageapi.service.snapshots.UserFolderSnapshotFileEntryService;
import com.victor.filestorageapi.service.utils.PathValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFolderSnapshotFileEntryImpl implements UserFolderSnapshotFileEntryService {

    private final ObjectQueryService objectQueryService;
    private final PathValidator pathValidator;

    @Override
    public UserFolderSnapshotFileEntry buildFileEntry(S3Object s3Object, UserFolderSnapshot folderSnapshot) {
        log.info("Building file entry snapshot for: {}", s3Object.key());

        String key = s3Object.key();
        Long size = s3Object.size();
        String fileEntryName = pathValidator.getKeyName(key);
        String fileEntryType = objectQueryService.getObjectContentType(key);
        String versionId;

        try{
            versionId = objectQueryService.getObjectVersionId(key);;
        }catch (Exception e){
            log.error("Failed to get version id for S3 object: {}", key);
            throw new RuntimeException(String.format("Failed to get version id for S3 object: %s", key));
        }

        return UserFolderSnapshotFileEntry
                .builder()
                .s3Key(key)
                .s3VersionId(versionId)
                .snapshotFileType(fileEntryType)
                .snapshotFileName(fileEntryName)
                .snapshotFileSize(size)
                .userFolderSnapshot(folderSnapshot)
                .isDeletedInSnapshot(false)
                .build();

    }

}
