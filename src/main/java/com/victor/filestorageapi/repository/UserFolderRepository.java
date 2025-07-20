package com.victor.filestorageapi.repository;

import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.models.entities.UserFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface UserFolderRepository extends JpaRepository<UserFolder, UUID> {

    Optional<UserFolder> findByUserAndFolderPathAndIsDeletedFalse(User user, String targetPath);
    Optional<UserFolder> findByUserAndIdAndIsDeletedFalse(User user, UUID folderId);
    Optional<UserFolder> findByUserAndIdAndIsDeletedTrue(User user, UUID folderId);
    boolean existsByUserAndFolderPathAndIsDeletedFalse(User user, String oldKey);
    boolean existsByUserAndFolderPathAndIsDeletedTrue(User user, String path);

    UserFolder findByUserAndFolderPathAndIsDeletedTrue(User user, String path);

//    @Query("SELECT uf FROM UserFolder WHERE uf.user = :user AND uf.folderPath LIKE CONCAT(:path, '%') AND uf.isDeleted = FALSE")
//    List<UserFolder> findAllActiveDescendantsAndSelf(@Param("user")User user, @Param("path") String folderPath);

    List<UserFolder> findByUserAndFolderPathStartingWithAndIsDeletedFalse(User user, String targetFolderPath);
}
