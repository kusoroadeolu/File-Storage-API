package com.victor.filestorageapi.service.folder.impl;

import com.victor.filestorageapi.exception.NoSuchUserFileException;
import com.victor.filestorageapi.exception.NoSuchUserFolderException;
import com.victor.filestorageapi.exception.UserFolderDeletionException;
import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.models.entities.UserFile;
import com.victor.filestorageapi.models.entities.UserFolder;
import com.victor.filestorageapi.repository.UserFileRepository;
import com.victor.filestorageapi.repository.UserFolderRepository;
import com.victor.filestorageapi.service.aws.ObjectDeleteService;
import com.victor.filestorageapi.service.aws.ObjectStorageService;
import com.victor.filestorageapi.service.folder.UserFolderDeleteService;
import com.victor.filestorageapi.service.snapshots.UserFolderSnapShotService;
import com.victor.filestorageapi.service.user.UserQueryService;
import com.victor.filestorageapi.service.utils.UserFolderUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserFolderDeletionServiceImpl implements UserFolderDeleteService {

    private final UserFolderRepository userFolderRepository;
    private final UserFileRepository userFileRepository;
    private final UserFolderSnapShotService userFolderSnapShotService;
    private final ObjectDeleteService objectDeleteService;
    private final UserFolderUtils folderUtils;
    private final UserQueryService userQueryService;

    //Soft delete
    @Override
    @Transactional
    public UserFolder softDeleteFolder(UUID userId, UUID folderId) {
        User user =  userQueryService.findUserById(userId);

        if (folderId == null) {
            log.warn("Folder ID cannot be null for user: {}", userId);
            throw new UserFolderDeletionException("Failed to delete target folder because folder ID is null");
        }

        UserFolder targetFolder =
                userFolderRepository.findByUserAndIdAndIsDeletedFalse(user, folderId)
                        .orElseThrow(() -> new NoSuchUserFolderException(
                                String.format("User Folder with ID: %s for user: %s was not found in the database or is already deleted.", folderId, user.getUsername())
                        ));
        log.info("Found target folder: {} (ID: {}) for user: {}", targetFolder.getName(), targetFolder.getId(), user.getUsername());

        String deleteMarkerVersionId = null; // Will store the S3 delete marker version ID

        try {
            //Create a delete marker in S3 (soft delete in S3)
            deleteMarkerVersionId = objectDeleteService.softDeleteObject(targetFolder.getFolderPath());

            //  Update the folder status in the database (soft delete in DB)
            log.info("Attempting soft delete operation on folder: {} (ID: {}) for saving in DB for user: {}", targetFolder.getName(), targetFolder.getId(), user.getUsername());
            targetFolder.setIsDeleted(true);
            targetFolder.setCurrentVersion(deleteMarkerVersionId);// Store the delete marker's version ID for potential restore
            targetFolder.setDeletedAt(LocalDateTime.now());
            log.info("Soft delete attempt on folder: {} (ID: {}) to be saved was successful for user: {}", targetFolder.getName(), targetFolder.getId(), user.getUsername());
            return targetFolder;

        }catch (Exception e) {
            log.error("An unexpected error occurred while trying to perform soft delete on folder: {} (ID: {}) for user: {}. Error: {}", targetFolder.getFolderPath(), targetFolder.getId(), user.getUsername(), e.getMessage(), e);
            //  S3 delete marker might have been created, but DB save failed due to unexpected error.
            // Attempt to remove the delete marker to "undelete" the S3 object.
            folderUtils.handleGeneralException(objectDeleteService, e, targetFolder.getFolderPath() ,deleteMarkerVersionId, user);
            throw new UserFolderDeletionException("An unexpected error occurred while trying to soft delete folder: " + targetFolder.getFolderPath(), e);
        }
    }

    @Override
    @Transactional
    public void recursiveSoftDeleteFolder(UUID userId, UUID folderId) {
        User user = userQueryService.findUserById(userId);

        if (folderId == null) {
            log.warn("Folder ID cannot be null for user: {}", userId);
            throw new UserFolderDeletionException("Failed to delete target folder because folder ID is null");
        }

        UserFolder folderToBeDeleted =
                userFolderRepository.findByUserAndIdAndIsDeletedFalse(user, folderId)
                        .orElseThrow(() -> new NoSuchUserFolderException(
                                String.format("User Folder with ID: %s for user: %s was not found in the database or is already deleted.", folderId, user.getUsername())
                        ));
        log.info("Found target folder: {} (ID: {}) for user: {}", folderToBeDeleted.getName(), folderToBeDeleted.getId(), user.getUsername());

        String folderPathToBeDeleted = folderToBeDeleted.getFolderPath();
        List<UserFolder> allActiveFolderDescendants = userFolderRepository.findByUserAndFolderPathStartingWithAndIsDeletedFalse(user, folderPathToBeDeleted);
        List<UserFile> allActiveFileDescendants = userFileRepository.findByUserAndFilePathStartingWithAndAndIsDeletedFalse(user, folderPathToBeDeleted);

        //Maps each soft deleted folder to its current version
        Map<String, String> mapObjectToDeleteMarker = new HashMap<>();

        //Maps all the active descendants of the prefix to their user file or folder
        Map<String, UserFolder> mapFolderPathToUserFolder = allActiveFolderDescendants.stream()
                .collect(Collectors.toMap(UserFolder::getFolderPath, folder -> folder));
        Map<String, UserFile> mapFilePathToUserFile = allActiveFileDescendants.stream()
                .collect(Collectors.toMap(UserFile::getFilePath, file -> file));


        try{
            mapObjectToDeleteMarker = objectDeleteService.putDeleteMarkersOnAllObjectsUnderPrefix(folderPathToBeDeleted);
        }catch (S3Exception e){
            log.error("An S3 error occurred while trying to batch delete objects under prefix: {}", folderPathToBeDeleted);
            //Compensation logic to roll back soft deletes
            rollBackObjectSoftDeletes(mapObjectToDeleteMarker, e, user);
            throw new UserFolderDeletionException(String.format("An S3 error occurred while trying to batch delete objects under prefix: %s", folderPathToBeDeleted));
        }catch (Exception e){
            log.error("An unexpected error occurred while trying to batch delete objects under prefix: {}", folderPathToBeDeleted);
            rollBackObjectSoftDeletes(mapObjectToDeleteMarker, e, user);
            throw new UserFolderDeletionException(String.format("An unexpected error occurred while trying to batch delete objects under prefix: %s", folderPathToBeDeleted));
        }

        try{
            LocalDateTime deletedAt = LocalDateTime.now();
            List<UserFolder> foldersToBeSaved = new ArrayList<>();
            List<UserFile> filesToBeSaved = new ArrayList<>();

            for(Map.Entry<String, String> entrySet: mapObjectToDeleteMarker.entrySet()){
                String s3Key = entrySet.getKey();
                String deleteMarkerVersionId = entrySet.getValue();


                if(mapFolderPathToUserFolder.containsKey(s3Key)){
                    UserFolder folderToBeSaved = mapFolderPathToUserFolder.get(s3Key);
                    folderToBeSaved.setIsDeleted(true);
                    folderToBeSaved.setCurrentVersion(deleteMarkerVersionId);
                    folderToBeSaved.setDeletedAt(deletedAt);
                    foldersToBeSaved.add(folderToBeSaved);
                }else if(mapFilePathToUserFile.containsKey(s3Key)){
                    UserFile fileToBeSaved = mapFilePathToUserFile.get(s3Key);
                    fileToBeSaved.setIsDeleted(true);
                    fileToBeSaved.setCurrentVersion(deleteMarkerVersionId);
                    fileToBeSaved.setDeletedAt(deletedAt);
                    filesToBeSaved.add(fileToBeSaved);
                }else {
                    log.warn("Found orphaned S3 object: {}. Manual intervention might be needed. ", s3Key);
                }
            }

            userFolderRepository.saveAll(foldersToBeSaved);
            userFileRepository.saveAll(filesToBeSaved);
        } catch (DataAccessException e) {
            log.error("Failed to save soft delete action for folder: {} (ID: {}) for user: {}. Database error: {}", folderToBeDeleted.getFolderPath(), folderToBeDeleted.getId(), user.getUsername(), e.getMessage(), e);
            rollBackObjectSoftDeletes(mapObjectToDeleteMarker, e, user);
            throw new UserFolderDeletionException(
                    String.format("Failed to save soft delete action for folder: %s. Database error occurred.", folderToBeDeleted.getFolderPath()),
                    e
            );

        } catch (Exception e) {
            log.error("An unexpected error occurred while trying to perform soft delete on folder: {} (ID: {}) for user: {}. Error: {}", folderToBeDeleted.getFolderPath(), folderToBeDeleted.getId(), user.getUsername(), e.getMessage(), e);
            rollBackObjectSoftDeletes(mapObjectToDeleteMarker, e, user);
            throw new UserFolderDeletionException("An unexpected error occurred while trying to soft delete folder: " + folderToBeDeleted.getFolderPath(), e);
        }

    }

    private void rollBackObjectSoftDeletes(Map<String, String> mapFolderToDeleteMarker, Exception e, User user){
        for(Map.Entry<String, String> entrySet: mapFolderToDeleteMarker.entrySet()){
            String folderPath = entrySet.getKey();
            String deleteMarker = entrySet.getValue();
            if(e instanceof DataAccessException){
                folderUtils.handleDataAccessException(objectDeleteService, (DataAccessException) e, folderPath ,deleteMarker, user);
            }else {
                folderUtils.handleGeneralException(objectDeleteService, e, folderPath ,deleteMarker, user);
            }
        }
    }


}
