package com.victor.filestorageapi.service.folder.impl;

import com.victor.filestorageapi.exception.*;
import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.models.entities.UserFile;
import com.victor.filestorageapi.models.entities.UserFolder;
import com.victor.filestorageapi.repository.UserFileRepository;
import com.victor.filestorageapi.repository.UserFolderRepository;
import com.victor.filestorageapi.service.aws.ObjectDeleteService;
import com.victor.filestorageapi.service.aws.ObjectStorageService;
import com.victor.filestorageapi.service.folder.UserFolderMoveService;
import com.victor.filestorageapi.service.snapshots.UserFolderSnapShotService;
import com.victor.filestorageapi.service.user.UserQueryService;
import com.victor.filestorageapi.service.utils.PathValidator;
import com.victor.filestorageapi.service.utils.UserFileUtils;
import com.victor.filestorageapi.service.utils.UserFolderUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFolderMoveServiceImpl implements UserFolderMoveService {

    private final UserFolderRepository userFolderRepository;
    private final ObjectStorageService objectStorageService;
    private final ObjectDeleteService objectDeleteService;
    private final UserFileRepository userFileRepository;
    private final UserFolderUtils folderUtils;
    private final UserFolderSnapShotService userFolderSnapShotService;
    private final UserFileUtils fileUtils;
    private final PathValidator pathValidator;
    private final UserQueryService userQueryService;

    /**
     * Moves a folder and its sub folders to a different one
     * NOTE: This operation permanently deletes the old/previous objects in S3 so retrieval operations cannot be carried out on moved objects
     * @param userId The id of the user for the operation
     * @param folderId The id of the folder to be moved
     * @param newParentFolderId The id of the folder that the folder will move to
     * */
    @Transactional
    @Override
    public void moveFolder(UUID userId, UUID folderId, UUID newParentFolderId){
        User user = userQueryService.findUserById(userId);

        //Ensure the target folder to be moved exists before moving it
        UserFolder targetFolder =
                userFolderRepository.findByUserAndIdAndIsDeletedFalse(user, folderId).orElseThrow(() ->
                        new NoSuchUserFolderException(
                                String.format("No folder with ID: (%s) belonging to user: (%s) was found", folderId, user.getUsername())
                        ));


        if(targetFolder.getIsRoot()){
            log.warn("Cannot move the root folder into another folder");
            throw new UserFolderMoveException("Cannot move the root folder into another folder");
        }


        log.info("Found target folder: {}", targetFolder.getFolderPath());

        //Get the old parent folder for the target folder
        UserFolder oldParentFolder = targetFolder.getParentFolder();

        log.info("Found old parent folder: {}", oldParentFolder.getFolderPath());

        //Ensure the new parent folder exists before moving it
        UserFolder newParentFolder =
                userFolderRepository.findByUserAndIdAndIsDeletedFalse(user, newParentFolderId).orElseThrow(() ->
                        new NoSuchUserFolderException(
                                String.format("No folder with ID: (%s) belonging to user: (%s) was found", newParentFolderId, user.getUsername())
                        ));
        log.info("Found new parent folder: {}", newParentFolder.getFolderPath());

        //Idempotent check to ensure a folder cannot be moved into itself or its descendant
        if(newParentFolder.getFolderPath().startsWith(targetFolder.getFolderPath())){
            log.warn("Cannot move folder: {} because its new parent folder: {} is a descendant of itself.",
                    targetFolder.getFolderPath(), newParentFolder.getFolderPath());
            throw new UserFolderMoveException(
                    String.format("Cannot move folder '%s' into its own descendant folder '%s'.",
                            targetFolder.getFolderPath(), newParentFolder.getFolderPath())
            );
        }

        //Constructing new folder path for the targetFolder
        String targetFolderPath = targetFolder.getFolderPath();
        String targetFolderCurrentVersionId = targetFolder.getCurrentVersion();
        String targetFolderName = targetFolder.getName();

        //Ensure the target folder is a sub folder of the parent folder
        if(!targetFolderPath.startsWith(oldParentFolder.getFolderPath())){
            log.error("Target folder's path {} does not start with its declared old parent's path {}.",
                    targetFolderPath, oldParentFolder.getFolderPath());
            throw new UserFolderMoveException(
                    String.format("Folder %s is not correctly associated with its parent %s. Cannot proceed with move.",
                            targetFolderPath, oldParentFolder.getFolderPath())
            );
        }

        log.info("Extracted target folder path: {}", targetFolderPath);

        //Build the new folder path for the main parent folder
        String newFolderPath = pathValidator.buildS3Key(newParentFolder.getFolderPath(), targetFolderName);
        log.info("Built new folder path: {}", newFolderPath);


        //Check for the existence of folder in the DB with the new folder path
        log.info("Checking existence of folder path: {} for user: {} in DB", newFolderPath, user);
        Optional<UserFolder> existingUserFolder = userFolderRepository
                .findByUserAndFolderPathAndIsDeletedFalse(user, newFolderPath);

        String newObjectVersionId = null;
        if(existingUserFolder.isPresent()) {
            log.info("Found existing folder: {} for user: {} in the DB", newFolderPath, user.getUsername());
            throw new UserFolderAlreadyExistsException(
                    String.format("The move operation could not be completed because folder (%s) already exists.", newFolderPath)
            );
        }


        //Get the user folders and files starting with the target folder from the db
        List<UserFolder> allActiveDescendantFolders = userFolderRepository.findByUserAndFolderPathStartingWithAndIsDeletedFalse(user, targetFolderPath);
        List<UserFile> allActiveDescendantFiles = userFileRepository.findByUserAndFilePathStartingWithAndAndIsDeletedFalse(user, targetFolderPath);


        Map<String, String> mapNewKeyAndVersionToOldKey = new HashMap<>();
        Map<String, String> mapNewKeyToS3Version = new HashMap<>();
        Map<String, String> mapOldKeyToVersionId = new HashMap<>();

        for (UserFile descendantFile: allActiveDescendantFiles){
            if(descendantFile.getCurrentVersion() != null){
                mapOldKeyToVersionId.put(descendantFile.getFilePath(), descendantFile.getCurrentVersion());
            }
        }

        for (UserFolder descendantFolder: allActiveDescendantFolders){
            //Ensures the current version is not null and skips the addition of the
            if(descendantFolder.getCurrentVersion() != null){
                mapOldKeyToVersionId.put(descendantFolder.getFolderPath(), descendantFolder.getCurrentVersion());
            }
        }

        try{
            //First move operation for the main target object, after then, its children are moved too
            //NOTE: new folder path and s3 new object key are the same
            newObjectVersionId = objectStorageService.copyS3ObjectVersion(newFolderPath, targetFolderPath, targetFolder.getCurrentVersion(), "");

            //List to keep track of new S3 keys
            log.info("Starting S3 move operation for {}", targetFolderPath);

            //Concatenate the folderPath and the version id so we can split them later
            String concatVersion = newFolderPath + "|" + newObjectVersionId;
            mapNewKeyAndVersionToOldKey.put(concatVersion, targetFolderPath);
            mapNewKeyToS3Version.put(newFolderPath, newObjectVersionId);
            mapOldKeyToVersionId.put(targetFolderPath, targetFolderCurrentVersionId);

            //Old key represents the key/path for the object we want to move
            for(Map.Entry<String, String> entrySet: mapOldKeyToVersionId.entrySet()){
                String oldS3Key = entrySet.getKey();
                String currentKeyVersionId = entrySet.getValue();

                if(oldS3Key.equals(targetFolderPath))continue;

                //Construct the new key(object destination)
                String newS3Key = oldS3Key.replaceFirst(targetFolderPath, newFolderPath);
                //TODO content type?
                String contentType = pathValidator.getContentTypeSafe(newS3Key);

                //Copy the new object and
                newObjectVersionId = objectStorageService.copyS3ObjectVersion(newS3Key, oldS3Key, currentKeyVersionId, contentType);
                concatVersion = newS3Key + "|" + newObjectVersionId;
                mapNewKeyAndVersionToOldKey.put(concatVersion, oldS3Key);
                mapNewKeyToS3Version.put(newS3Key, newObjectVersionId);
            }
            log.info("Successfully moved {} new keys from {} to {}", mapNewKeyToS3Version.size(), targetFolderPath, newFolderPath);

            //After successful moves, attempt to delete the original objects
            log.info("Starting S3 delete operation for folder: {} and its children", targetFolderPath);
            List<String> failedDeletions = new ArrayList<>();

            for (Map.Entry<String , String> entrySet: mapOldKeyToVersionId.entrySet()){
                String oldS3Key = entrySet.getKey();
                String keyVersionId = entrySet.getValue();

                try{
                    objectDeleteService.permanentDeleteObjectVersion(oldS3Key, keyVersionId);
                    log.info("Successfully deleted S3 key: {} (VERSION: {})", oldS3Key, keyVersionId);
                }catch (Exception e){
                    log.info("Encountered an S3 error while trying to permanently delete: {} (VERSION: {})", oldS3Key, keyVersionId);
                    failedDeletions.add(String.format("Failed to permanently delete original S3 object version '%s' (Version ID: '%s') during move. Data might be duplicated.", oldS3Key, keyVersionId));

                }
            }

            //If the deletions didn't complete log the error and throw an exception
            if(!failedDeletions.isEmpty()){
                String errorMessage = String.format("Completed S3 move, but some original objects failed to delete: %s. Data might be duplicated.", String.join(", ", failedDeletions));
                log.error(errorMessage);
                throw new UserFolderMoveException(errorMessage);
            }

            log.info("Finished S3 soft delete operation for original objects.");

        }catch (S3Exception e){
            log.error("Encountered an S3 error while trying to move folder: {} to {}", targetFolderPath, newFolderPath);

            for(Map.Entry<String, String> entryMap: mapNewKeyToS3Version.entrySet()){
                String newKey = entryMap.getKey();
                String versionId = entryMap.getValue();
                folderUtils.handleGeneralException(objectDeleteService, e, newKey, versionId, user);
            }

            throw new UserFolderMoveException(String.format("Encountered an S3 error while trying to move folder: %s to %s", targetFolderPath, newFolderPath));
        }catch (Exception e) {
            log.error("Encountered an unexpected error while trying to move folder: {} to {}", targetFolderPath, newFolderPath);

            for(Map.Entry<String, String> entryMap: mapNewKeyToS3Version.entrySet()){
                String newKey = entryMap.getKey();
                String versionId = entryMap.getValue();
                folderUtils.handleGeneralException(objectDeleteService, e, newKey, versionId, user);
            }

            throw new UserFolderMoveException(String.format("Encountered an unexpected error while trying to move folder: %s to %s", targetFolderPath,newFolderPath));
        }

        //
        Map<String, UserFolder> mapFolderPathToUserFolder = allActiveDescendantFolders.stream()
                .collect(Collectors.toMap(UserFolder::getFolderPath, folder -> folder));
        Map<String, UserFile> mapFilePathToUserFile = allActiveDescendantFiles.stream()
                .collect(Collectors.toMap(UserFile::getFilePath, file -> file));

        //After the S3 operation completes successfully, we deal with the DB
        try{
            log.info("Attempting move operation for folder: {} in the DB", targetFolderPath);

            List<UserFolder> finalFoldersToBeSaved = new ArrayList<>();
            List<UserFile> finalFilesToBeSaved = new ArrayList<>();


            for(Map.Entry<String, String> entryMap: mapNewKeyAndVersionToOldKey.entrySet()){
                String oldKey = entryMap.getValue();
                String newKey = separateKeyFromVersion(entryMap.getKey());
                String newVersionId = separateVersionFromKey(entryMap.getKey());

                //Extract the parent folder path and get the parent folder
                String immediateNewParentFolderPath = pathValidator.extractParentFolderPathFromKey(newKey);

                //Check if the immediate parent folder is empty, if so default to the new parent folder
                UserFolder immediateNewParentFolder =
                            immediateNewParentFolderPath != null && immediateNewParentFolderPath.isEmpty() ?
                                    newParentFolder : userFolderRepository.findByUserAndFolderPathAndIsDeletedFalse(user, immediateNewParentFolderPath)
                                    .orElseThrow(() -> new NoSuchUserFolderException(
                                            String.format("Immediate parent folder '%s' for moved object '%s' not found for user '%s'. This indicates a data inconsistency.",
                                                    immediateNewParentFolderPath, newKey, user.getUsername())
                                    ));


                LocalDateTime updatedAt = LocalDateTime.now();

                //Checks if an active user entity exists in their respective hashmaps, if so perform the move operation on it in the DB
                if(mapFilePathToUserFile.containsKey(oldKey)){
                    UserFile userFile = mapFilePathToUserFile.get(oldKey);
                    userFile.setUserFolder(immediateNewParentFolder);
                    userFile.setCurrentVersion(newVersionId);
                    userFile.setFilePath(newKey);
                    userFile.setUpdatedAt(updatedAt);
                    finalFilesToBeSaved.add(userFile);

                }else if(mapFolderPathToUserFolder.containsKey(oldKey)) {
                    UserFolder userFolder = mapFolderPathToUserFolder.get(oldKey);
                    userFolder.setFolderPath(newKey);
                    userFolder.setParentFolder(immediateNewParentFolder);
                    userFolder.setCurrentVersion(newVersionId);
                    userFolder.setUpdatedAt(updatedAt);
                    finalFoldersToBeSaved.add(userFolder);
                }else {
                    log.warn("No DB entity found for moved S3 key '{}'. Might be an orphan or inconsistent state.", oldKey);
                }

            }

            //If the save operation is not successful, we'll revert the move operation
            userFolderRepository.saveAll(finalFoldersToBeSaved);
            userFileRepository.saveAll(finalFilesToBeSaved);
            log.info("Successfully saved move action for folder: {} (ID: {}) for user: {}", targetFolder.getFolderPath(), targetFolder.getId(), user.getUsername());


        }catch (DataAccessException e) {
            log.error("Failed to save move action for folder: {} (ID: {}) for user: {}. Database error: {}", targetFolder.getFolderPath(), targetFolder.getId(), user.getUsername(), e.getMessage(), e);
            // S3 delete marker has been created, but DB save failed.
            // Attempt to remove the delete marker to "undelete" the S3 object.
            revertMoveOperation(mapNewKeyAndVersionToOldKey);
            throw new UserFolderMoveException(
                    String.format("Failed to save soft delete action for folder: %s. Database error occurred.", targetFolder.getFolderPath()), e
            );
        } catch (Exception e) {
            log.error("An unexpected error occurred while trying to perform move on folder: {} (ID: {}) for user: {}. Error: {}", targetFolder.getFolderPath(), targetFolder.getId(), user.getUsername(), e.getMessage(), e);
            //  S3 delete marker might have been created, but DB save failed due to unexpected error.
            // Attempt to remove the delete marker to "undelete" the S3 object.
            revertMoveOperation(mapNewKeyAndVersionToOldKey);
            throw new UserFolderMoveException("An unexpected error occurred while trying to soft delete folder: " + targetFolder.getFolderPath(), e);
        }

    }

    //Reverts a failed move operation
    private void revertMoveOperation(Map<String, String> mapNewKeyAndVersionToOldKey) {
        int numberOfRetries = 5;
        int retryDelay = 10000;

        // To ensure old objects are back before deleting new ones, process in two sub-phases
        // If delete fails, that's a duplication, less critical than not having the original back.

        while (numberOfRetries >= 0) {
            boolean allCopiesBackSuccessful = true;
            List<String> successfullyCopiedBackOldKeys = new ArrayList<>(); // To track what was restored

            try {
                log.info("Attempting to restore old objects after DB save failure. Size: {}", mapNewKeyAndVersionToOldKey.size());
                for (Map.Entry<String, String> entrySet : mapNewKeyAndVersionToOldKey.entrySet()) {
                    String newObjectKey = separateKeyFromVersion(entrySet.getKey()); // Extract new key
                    String newObjectVersionId = separateVersionFromKey(entrySet.getKey()); // Extract version from new key
                    String oldObjectKey = entrySet.getValue(); // Get original old key

                    try {
                        String contentType = pathValidator.getContentTypeSafe(newObjectKey);
                        objectStorageService.copyS3ObjectVersion(oldObjectKey, newObjectKey, newObjectVersionId, contentType);
                        successfullyCopiedBackOldKeys.add(oldObjectKey); // Mark as successfully restored
                        log.debug("Successfully copied back from {} (Version: {}) to {}", newObjectKey, newObjectVersionId, oldObjectKey);
                    } catch (Exception copyBackEx) {
                        log.error("Failed to copy object back from {} to {}. This object might remain at the new location.", newObjectKey, oldObjectKey, copyBackEx);
                        allCopiesBackSuccessful = false;
                    }
                }

                if (!allCopiesBackSuccessful) {
                    log.error("Not all objects could be copied back to their original locations.");
                }

                log.info("Successfully copied all objects back. Attempting permanent delete on new objects.");
                for (Map.Entry<String, String> entrySet : mapNewKeyAndVersionToOldKey.entrySet()) {
                    String newObjectKey = separateKeyFromVersion(entrySet.getKey());
                    String newObjectVersionId = separateVersionFromKey(entrySet.getKey());
                    try {
                        objectDeleteService.permanentDeleteObjectVersion(newObjectKey, newObjectVersionId);
                        log.debug("Successfully permanently deleted new object: {} (Version: {})", newObjectKey, newObjectVersionId);
                    } catch (Exception deleteNewEx) {
                        log.error("CRITICAL: Failed to delete new object {} (Version: {}) after successful copy back. Data inconsistency: Duplication.", newObjectKey, newObjectVersionId, deleteNewEx);
                        // This is a serious issue; duplication. Manual intervention needed.
                        // Don't re-throw, let loop finish or handle outside.
                    }
                }
                log.info("Successfully reverted S3 move operation.");
                return; // Exit on success

            } catch (Exception e) { // Catch any exception during the revert process
                log.warn("We encountered an error while trying to revert the move operation. Number of retries left: {}. Error: {}", numberOfRetries, e.getMessage());
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread sleep interrupted during revert retry.", ie);
                }

                numberOfRetries--;
                retryDelay = Math.max(1000, retryDelay / 2); // Exponential backoff for retries
            }
        }
        log.error("Failed to fully revert S3 move operation after multiple retries. Manual intervention required for data consistency.");

    }



    private String separateKeyFromVersion(String concat){
        int lastIndex = concat.lastIndexOf("|");
        if (lastIndex == -1) { // If no separator, or other invalid format
            log.error("Invalid concatenated key-version string format (missing separator): {}", concat);
            throw new IllegalArgumentException("Invalid key-version format: " + concat); // Or handle as per your error strategy
        }
        return concat.substring(0, lastIndex);
    }

    private String separateVersionFromKey(String concat){
        int lastIndex = concat.lastIndexOf("|");
        if (lastIndex == -1) { // If no separator, or other invalid format
            log.error("Invalid concatenated key-version string format (missing separator): {}", concat);
            throw new IllegalArgumentException("Invalid key-version format: " + concat); // Or handle as per your error strategy
        }
        return concat.substring(lastIndex + 1);
    }


}
