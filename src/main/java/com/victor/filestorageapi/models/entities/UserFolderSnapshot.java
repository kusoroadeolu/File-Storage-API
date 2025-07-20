package com.victor.filestorageapi.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(name = "folder_snapshot")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserFolderSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_folder_id", nullable = false)
    private UUID userFolderId;

    @Column(name = "folder_path", nullable = false, length = 1024)
    private String folderPath;

    @Column(name = "folder_snapshot_version_id", nullable = false)
    private String folderSnapShotVersionId;

    @Column(length = 50)
    private String description;

    @Column(name = "snapshot_timestamp", nullable = false, updatable = false)
    private LocalDateTime snapshotTimestamp;

    @OneToMany(mappedBy = "userFolderSnapshot", cascade = CascadeType.ALL, orphanRemoval = true ,fetch = FetchType.LAZY)
    private List<UserFolderSnapshotFileEntry> fileEntries;
}
