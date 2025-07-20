package com.victor.filestorageapi.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long fileSize;

    @ManyToOne
    @JoinColumn(name = "folder_id")
    private UserFolder userFolder;

    private String fileName;

    private String fileType;

    private String filePath;

    private String currentVersion;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    private LocalDateTime updatedAt;

    private Boolean isDeleted = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

}
