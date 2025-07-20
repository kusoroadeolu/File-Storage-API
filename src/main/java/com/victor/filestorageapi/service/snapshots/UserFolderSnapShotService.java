package com.victor.filestorageapi.service.snapshots;

import com.victor.filestorageapi.models.entities.UserFolder;
import com.victor.filestorageapi.models.entities.UserFolderSnapshot;
import org.springframework.stereotype.Service;

@Service
public interface UserFolderSnapShotService {
    public UserFolderSnapshot createFolderSnapshot(UserFolder userFolder);
}
