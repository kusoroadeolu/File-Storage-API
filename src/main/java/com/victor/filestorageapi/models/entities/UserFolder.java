package com.victor.filestorageapi.models.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_folder",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"userId", "folderPath", "isDeleted"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Column(unique = true)
    private String folderPath;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_folder_id")
    @ToString.Exclude
    private UserFolder parentFolder;

    @OneToMany(mappedBy = "parentFolder")
    @ToString.Exclude
    private List<UserFolder> subFolders = new ArrayList<>();

    @OneToMany(mappedBy = "userFolder")
    @ToString.Exclude
    private List<UserFile> userFiles = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @ColumnDefault("false")
    private Boolean isRoot;

    @ColumnDefault("false")
    private Boolean isDeleted;

    private String currentVersion;


    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    @PreUpdate
    public void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }


}
