package com.victor.filestorageapi.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "folder_snapshot_file_entry")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserFolderSnapshotFileEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String s3VersionId;

    @Column(nullable = false)
    private String s3Key;

    @Column(name = "snapshot_file_name", nullable = false)
    private String snapshotFileName;

    @Column(name = "snapshot_file_type")
    private String snapshotFileType;

    @Column(name = "snapshot_file_size")
    private Long snapshotFileSize;

    @Column(name = "is_deleted_in_snapshot", nullable = false)
    private boolean isDeletedInSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_snapshot", nullable = false)
    private UserFolderSnapshot userFolderSnapshot;



}
