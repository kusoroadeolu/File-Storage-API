package com.victor.filestorageapi.repository;

import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.models.entities.UserFile;
import com.victor.filestorageapi.models.entities.UserFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFileRepository extends JpaRepository<UserFile, UUID> {

    Optional<UserFile> findByUserAndFilePathAndIsDeletedFalse(User user, String filePath);



    boolean existsByUserFolderAndFilePathAndIsDeletedFalse(UserFolder userFolder, String filePath);

    boolean existsByUserAndFilePathAndIsDeletedFalse(User user, String filePath);


    UserFile findByUserAndFilePathAndIsDeletedTrue(User user, String path);

    List<UserFile> findByUserAndFilePathStartingWithAndAndIsDeletedFalse(User user, String targetFolderPath);
}
