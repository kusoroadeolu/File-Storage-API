package com.victor.filestorageapi.service.folder.impl;

import com.victor.filestorageapi.exception.MyCustomS3Exception;
import com.victor.filestorageapi.exception.NoSuchUserFolderException;
import com.victor.filestorageapi.exception.UserFolderCreationException;
import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.models.entities.UserFolder;
import com.victor.filestorageapi.repository.UserFolderRepository;
import com.victor.filestorageapi.service.aws.ObjectDeleteService;
import com.victor.filestorageapi.service.aws.ObjectStorageService;
import com.victor.filestorageapi.service.folder.UserFolderCreationService;
import com.victor.filestorageapi.service.snapshots.UserFolderSnapShotService;
import com.victor.filestorageapi.service.user.UserCommandService;
import com.victor.filestorageapi.service.user.UserQueryService;
import com.victor.filestorageapi.service.utils.PathValidator;
import com.victor.filestorageapi.service.utils.UserFolderUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserFolderCreationServiceImpl implements UserFolderCreationService {
    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final UserFolderRepository userFolderRepository;
    private final ObjectStorageService objectStorageService;
    private final ObjectDeleteService objectDeleteService;
    private final UserFolderSnapShotService userFolderSnapShotService;
    private final UserFolderUtils folderUtils;
    private final PathValidator pathValidator;


    @Transactional
    @Override
    public UserFolder createRootFolder(UUID userId) {

        User user = userQueryService.findUserById(userId);
        log.info("Searching for existing root folder for user {}", user.getUsername());
        UserFolder existingRootFolder = user.getRootFolder();

        if (existingRootFolder != null) {
            log.info("Found existing root folder for user {}. Returning existing folder.", user.getUsername());
            return existingRootFolder;
        }

        String s3ObjectVersion = null;
        String rootS3Key = pathValidator.buildS3Key("", userId.toString());


        try {
            log.info("Attempting to create root object for user {} in S3.", user.getUsername());
            s3ObjectVersion = objectStorageService.createObject(user, rootS3Key);
            log.info("Successfully created root object in S3 for user {}. Version ID: {}", user.getUsername(), s3ObjectVersion);

            log.info("Attempting to create new root folder in DB for user {}", user.getUsername());
            UserFolder newRootFolder = UserFolder
                    .builder()
                    .folderPath(rootS3Key)
                    .name(userId.toString())
                    .parentFolder(null) // Root folder has no parent
                    .isRoot(true)
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .currentVersion(s3ObjectVersion)
                    .build();

            //Save the folder version properties for this folder
            UserFolder savedRootFolder = userFolderRepository.save(newRootFolder);
            userCommandService.assignRootFolder(user, savedRootFolder);

            log.info("Successfully saved root folder to DB for user {}. Folder ID: {}", user.getUsername(), savedRootFolder.getId());
            return savedRootFolder;

        } catch (DataIntegrityViolationException ex) {
            // This catches a race condition where another transaction created the root folder concurrently
            log.warn("Attempted to create another root folder for user: {} (ID: {}). A concurrent creation likely occurred.", user.getUsername(), userId, ex);
            // S3 object might have been created but DB save failed due to uniqueness.
            // Attempt compensation by cleaning up the S3 object.
            folderUtils.handleDataIntegrityException(objectDeleteService, ex, userId.toString() ,s3ObjectVersion ,user);
            throw new UserFolderCreationException("Failed to create a new root folder due to a duplicate entry in the database.", ex);

        }catch (S3Exception se) {
            // Catches S3-specific errors during root object creation
            log.error("An S3 error occurred while trying to create the root folder for user: {} (ID: {}). Error: {}", user.getUsername(), userId, se.getMessage(), se);
            throw new MyCustomS3Exception("An S3 error occurred while trying to create the root folder for user: " + user.getUsername(), se);
        }catch (Exception e) {
            // Catches any other unexpected errors during S3 creation or DB save
            log.error("An unexpected error occurred while trying to create root folder for user: {} (ID: {}). Error: {}", user.getUsername(), userId, e.getMessage(), e);
            // CRITICAL: S3 object might have been created but DB save failed due to unexpected error.
            // Attempt compensation by cleaning up the S3 object.
            folderUtils.handleGeneralException(objectDeleteService, e, userId.toString() ,s3ObjectVersion, user);
            throw new UserFolderCreationException("An unexpected error occurred while trying to create root folder for user: " + user.getUsername(), e);
        }
    }

    @Transactional
    @Override
    public UserFolder createSubFolder(UUID userID, UUID parentFolderId, String folderName) {

        if (folderName == null || folderName.trim().isEmpty()) {
            log.warn("Folder name cannot be null or empty for user: {}", userID);
            throw new UserFolderCreationException("Folder name should not be null or empty");
        }

        folderName = folderName.trim();

        User user = userQueryService.findUserById(userID);
        UserFolder parentFolder;
        log.info("Attempting to get parent folder with id: {} for user: {}", parentFolderId, user.getUsername());

        // Check if a parent folder id was passed, if not default to root folder
        if (parentFolderId == null) {
            log.info("No parent folder id found. Defaulting to root folder for user: {}", user.getUsername());
            parentFolder = user.getRootFolder();
            if (parentFolder == null) {
                log.error("User {} (ID: {}) does not have a root folder. Cannot create subfolder.", user.getUsername(), userID);
                throw new UserFolderCreationException("User does not have a root folder. Please ensure the root folder is created first.");
            }
            log.info("Fallback to root folder: {} (ID: {}) for user: {}", parentFolder.getName(), parentFolder.getId(), user.getUsername());
        } else {
            parentFolder = userFolderRepository.findByUserAndIdAndIsDeletedFalse(user, parentFolderId)
                    .orElseThrow(() -> new NoSuchUserFolderException(
                            String.format("Parent folder with id: %s for user: %s was not found in the database or is deleted.", parentFolderId, user.getUsername())
                    ));

            // Check if the parent folder belongs to the user
            if (!parentFolder.getUser().getId().equals(userID)) {
                log.error("Security Alert: Parent folder: {} (ID: {}) does not belong to user: {} (ID: {}).", parentFolder.getName(), parentFolder.getId(), user.getUsername(), userID);
                throw new UserFolderCreationException("Parent folder with ID: " + parentFolderId + " does not belong to the requesting user.");
            }
            log.info("Successfully retrieved parent folder: {} (ID: {}) from DB for user: {}", parentFolder.getName(), parentFolder.getId(), user.getUsername());
        }

        // Construct the full S3 object key for the new folder
        log.info("Parent folder key: {}", parentFolder.getFolderPath());
        String newS3ObjectKey = pathValidator.buildS3Key(parentFolder.getFolderPath(), folderName);

        // Check if the new folder path already exists for the user in the db
        log.info("Checking for existing folder with path: {} in DB for user: {}", newS3ObjectKey, user.getUsername());
        Optional<UserFolder> optionalExistingFolder =
                userFolderRepository.findByUserAndFolderPathAndIsDeletedFalse(user, newS3ObjectKey);

        if (optionalExistingFolder.isPresent()) {
            log.info("Folder: {} already exists for user: {} in the DB. Returning existing folder.", newS3ObjectKey, user.getUsername());
            return optionalExistingFolder.get();
        }

        String s3ObjectVersion = null; // Will store the S3 object version ID

        try {
            // Create the object in S3
            s3ObjectVersion = objectStorageService.createObject(user, newS3ObjectKey);

            // Create the folder entry in the database

            log.info("Attempting to construct and save user folder to DB for user: {}", user.getUsername());
            UserFolder newFolder = UserFolder
                    .builder()
                    .name(folderName)
                    .folderPath(newS3ObjectKey)
                    .isRoot(false)
                    .parentFolder(parentFolder)
                    .isDeleted(false)
                    .user(user)
                    .currentVersion(s3ObjectVersion)
                    .createdAt(LocalDateTime.now())
                    .build();

            UserFolder savedFolder = userFolderRepository.save(newFolder);
            log.info("Successfully saved folder: {} (ID: {}) to DB for user {}", folderName, savedFolder.getId(), user.getUsername());
            return savedFolder;

        } catch (DataIntegrityViolationException ex) {
            log.warn("Attempted to create a duplicate folder: {} for user: {} in the DB after S3 creation. Concurrent creation likely occurred.", newS3ObjectKey, user.getUsername(), ex);
            // S3 object has been created, but DB save failed due to uniqueness.
            // Attempt compensation by cleaning up the S3 object.
            folderUtils.handleDataIntegrityException(objectDeleteService, ex, newS3ObjectKey ,s3ObjectVersion, user);
            throw new UserFolderCreationException("Failed to create a new folder: " + folderName + " due to a duplicate entry in the database.", ex);

        } catch (S3Exception e) {
            // Catches S3-specific errors during object creation
            log.error("An S3 error occurred while trying to create folder: {} for user: {} in bucket: {}", folderName, user.getUsername(), e.getMessage(), e);
            throw new MyCustomS3Exception("An S3 error occurred while trying to create folder: " + folderName + " for user " + user.getUsername() , e);

        } catch (Exception e) {
            // Catches any other unexpected errors during S3 creation or DB save
            log.error("An unexpected error arose while trying to create a folder: {} for user: {}. Error: {}", folderName, user.getUsername(), e.getMessage(), e);
            // S3 object might have been created but DB save failed due to unexpected error.
            // Attempt compensation by cleaning up the S3 object.
            folderUtils.handleGeneralException(objectDeleteService, e, newS3ObjectKey ,s3ObjectVersion, user);
            throw new UserFolderCreationException("An unexpected error arose while trying to create a folder " + folderName + " for user: " + user.getUsername(), e);
        }
    }
}
