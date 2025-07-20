package com.victor.filestorageapi.repository;

import com.victor.filestorageapi.models.entities.UserFolderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserFolderSnapshotRepository extends JpaRepository<UserFolderSnapshot, UUID> {
}
