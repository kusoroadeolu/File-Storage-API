package com.victor.filestorageapi.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table
@NoArgsConstructor
@Data
public class UserFile {
    @Id
    @GeneratedValue
    private UUID id;

    private Long fileSize;

    @ManyToOne
    @JoinColumn(name = "folder_id")
    private UserFolder userFolder;

    private String fileName;
    private String fileType;

    @Column(updatable = false)
    private final LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime deletedAt;

    private LocalDateTime updatedAt;

    private Boolean isDeleted = false;
}
