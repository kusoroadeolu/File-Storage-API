package com.victor.filestorageapi.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table
@Data
@NoArgsConstructor
public class UserFolder {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    private Long folderSize;

    @ManyToOne
    @JoinColumn(name = "parent_folder_id")
    private UserFolder parentFolder;

    @OneToMany(mappedBy = "parentFolder", cascade = CascadeType.ALL)
    private List<UserFolder> subFolders = new ArrayList<>();

    @OneToMany(mappedBy = "userFolder", cascade = CascadeType.ALL)
    private List<UserFile> userFiles = new ArrayList<>();


    @Column(updatable = false)
    private final LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private Boolean isRoot = false;

    private Boolean isDeleted = false;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;
}
