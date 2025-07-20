package com.victor.filestorageapi.repository;

import com.victor.filestorageapi.models.entities.UserFolderSnapshotFileEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserFolderSnapshotFileEntryRepository extends JpaRepository<UserFolderSnapshotFileEntry, UUID> {
}
