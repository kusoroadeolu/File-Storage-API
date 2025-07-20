package com.victor.filestorageapi.models.entities;

import com.victor.filestorageapi.models.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String username;

    private Role role;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(updatable = false)
    private final LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "root_folder_id")
    @ToString.Exclude
    private UserFolder rootFolder;

    @OneToMany(mappedBy = "user")
    private List<UserFile> userFiles;

    public User(String username, String email, String password, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

}
