package com.victor.filestorageapi.service.snapshots;

import com.victor.filestorageapi.models.entities.UserFolderSnapshot;
import com.victor.filestorageapi.models.entities.UserFolderSnapshotFileEntry;
import software.amazon.awssdk.services.s3.model.S3Object;

public interface UserFolderSnapshotFileEntryService {

    UserFolderSnapshotFileEntry buildFileEntry(S3Object s3Object, UserFolderSnapshot folderSnapshot);
}
