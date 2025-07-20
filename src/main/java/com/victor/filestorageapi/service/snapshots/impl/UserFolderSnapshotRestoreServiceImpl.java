package com.victor.filestorageapi.service.snapshots.impl;

import com.victor.filestorageapi.exception.NoSuchSnapshotException;
import com.victor.filestorageapi.exception.NoSuchUserFolderException;
import com.victor.filestorageapi.exception.SnapshotRestoreException;
import com.victor.filestorageapi.models.FolderNode;
import com.victor.filestorageapi.models.entities.*;
import com.victor.filestorageapi.repository.UserFileRepository;
import com.victor.filestorageapi.repository.UserFolderRepository;
import com.victor.filestorageapi.repository.UserFolderSnapshotRepository;
import com.victor.filestorageapi.service.aws.ObjectDeleteService;
import com.victor.filestorageapi.service.aws.ObjectQueryService;
import com.victor.filestorageapi.service.aws.ObjectStorageService;
import com.victor.filestorageapi.service.folder.UserFolderDeleteService;
import com.victor.filestorageapi.service.snapshots.UserFolderSnapshotRestoreService;
import com.victor.filestorageapi.service.user.UserQueryService;
import com.victor.filestorageapi.service.utils.UserFileUtils;
import com.victor.filestorageapi.service.utils.UserFolderUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserFolderSnapshotRestoreServiceImpl implements UserFolderSnapshotRestoreService {

    private final UserQueryService userQueryService;
    private final UserFolderDeleteService userFolderDeleteService;
    private final ObjectQueryService objectQueryService;
    private final UserFolderUtils userFolderUtils;
    private final UserFolderSnapshotRepository userFolderSnapshotRepository;
    private final UserFileRepository userFileRepository;
    private final UserFolderRepository userFolderRepository;
    private final ObjectStorageService objectStorageService;
    private final ObjectDeleteService objectDeleteService;
    private final UserFileUtils userFileUtils;

    /**
     * Restores a folder's snapshot
     * @param userId The id of the user which the snapshot belongs to
     * @param snapshotId The snapshotId you want to return
     * @exception DataIntegrityViolationException
     * @exception S3Exception
     * */
    @Override
    @Transactional
    public void restoreFolderSnapshot(UUID userId, UUID snapshotId) {
        User user = userQueryService.findUserById(userId);

        UserFolderSnapshot snapshotToRestore = userFolderSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new NoSuchSnapshotException(String.format("Could not find folder snapshot with ID: %s", snapshotId)));
        log.info("Found snapshot: {} with ID: {}", snapshotToRestore.getFolderSnapShotVersionId(), snapshotId);

        UUID snapshotUserFolderId = snapshotToRestore.getUserFolderId();

        UserFolder userFolderBelongingToSnapshot = userFolderRepository.findByUserAndIdAndIsDeletedFalse(user, snapshotUserFolderId)
                .orElseThrow(() -> new NoSuchUserFolderException(String.format("Could not find user folder with id: %s", snapshotUserFolderId)));
        log.info("Found user folder: {} belonging to snapshot: {}", userFolderBelongingToSnapshot.getFolderPath(), snapshotToRestore.getFolderSnapShotVersionId());


        if(userFolderBelongingToSnapshot.getUser() != user){
            log.error("Snapshot {} does not belong to user: {}", snapshotToRestore.getFolderSnapShotVersionId(), user.getUsername());
            throw new SnapshotRestoreException(String.format("Snapshot %s does not belong to user: %s", snapshotToRestore.getFolderSnapShotVersionId(), user.getUsername()));
        }

        //A list of all the entries that will be restored in s3 and the db
        List<UserFolderSnapshotFileEntry> entriesToRestore = snapshotToRestore.getFileEntries();
        log.info("Number of entries to restore: {}. First entry: {}", entriesToRestore.size(), entriesToRestore.getFirst());

        //Gets all the objects under the current folders' prefix
        List<S3Object> allObjectsUnderPrefix =
                objectStorageService.listS3ObjectsByPrefix(userFolderBelongingToSnapshot.getFolderPath());
        log.info("Number of objects found under: {}. First Object: {}", allObjectsUnderPrefix.size(), allObjectsUnderPrefix.getFirst());

        //A map of each active descendant user file and user folder of the user folder for the snapshot
        Map<String ,UserFile> activeDescendantUserFiles =
                userFileRepository.findByUserAndFilePathStartingWithAndAndIsDeletedFalse(user, snapshotToRestore.getFolderPath())
                        .stream().collect(Collectors.toMap(UserFile::getFilePath, file -> file));
        Map<String ,UserFolder> activeDescendantUserFolders =
                userFolderRepository.findByUserAndFolderPathStartingWithAndIsDeletedFalse(user, snapshotToRestore.getFolderPath())
                        .stream().collect(Collectors.toMap(UserFolder::getFolderPath, folder -> folder));

        //Keeps track of the objects that have been restored.
        Map<String, String> restoredObjects = new HashMap<>();
        Map<String, String> deletedObjects = new HashMap<>();

        //Restore the current file entries in s3
        log.info("Attempting to restore each entry in snapshot: {}", snapshotToRestore.getFolderSnapShotVersionId());
        try{
            //Loops through each entry and restores the version in s3
            for(UserFolderSnapshotFileEntry fileEntry: entriesToRestore){
                String s3Key = fileEntry.getS3Key();
                String s3VersionId = fileEntry.getS3VersionId();

                log.info("Before restore version call.");
                //Get the versioned object and restore it
                String newVersionId = objectStorageService.retrieveVersionedObject(s3Key, s3VersionId);
                log.info("After restore version call.");
                restoredObjects.put(s3Key, newVersionId);
            }

            log.info("Successfully restored {} objects", restoredObjects.size());


            //Then delete objects that were not part of the restoration
            for (S3Object s3Object: allObjectsUnderPrefix){
                String s3Key = s3Object.key();

                if(restoredObjects.containsKey(s3Key)){
                    log.info("Object: {} was restored. Skipping deletion.", s3Key);
                    continue;
                }

                String deleteMarkerId = objectDeleteService.softDeleteObject(s3Key);
                deletedObjects.put(s3Key, deleteMarkerId);
            }
            log.info("Deleted {} S3 objects", deletedObjects.size());

        }catch (S3Exception e){
            log.error("An S3 error occurred while trying to restore snapshot: {} for user: {}", snapshotToRestore.getFolderSnapShotVersionId(), user.getUsername());
            rollBackSnapshotRestore(snapshotToRestore, deletedObjects, restoredObjects);
            throw new SnapshotRestoreException(String.format("An S3 error occurred while trying to restore snapshot: %s for user: %s", snapshotToRestore.getFolderSnapShotVersionId(), user.getUsername()));
        }catch (Exception e){
            log.error("An unexpected error occurred while trying to restore snapshot: {} for user: {}", snapshotToRestore.getFolderSnapShotVersionId(), user.getUsername());
            rollBackSnapshotRestore(snapshotToRestore, deletedObjects, restoredObjects);
            throw new SnapshotRestoreException(String.format("An unexpected error occurred while trying to restore snapshot: %s for user: %s", snapshotToRestore.getFolderSnapShotVersionId(), user.getUsername()));
        }

        //These maps will store UserFolder/File entities as they are saved/updated, also links children to their parents
        Map<String, UserFolder> mapPathToSavedFolder = new HashMap<>();
        List<String> entriesToRestoreKeys = snapshotToRestore.getFileEntries()
                .stream()
                .map(UserFolderSnapshotFileEntry::getS3Key)
                .collect(Collectors.toList());

        //Add the snapshot to the map
        mapPathToSavedFolder.put(snapshotToRestore.getFolderPath(), userFolderBelongingToSnapshot);


        var updatedAt = LocalDateTime.now();

        //Builds and properly arranges the folder tree, then flattens the tree
        FolderNode rootNodeOfSnapshot = userFolderUtils.buildFolderTree(snapshotToRestore.getFolderPath(), entriesToRestoreKeys);
        Map<String, S3Object> allRestoredObjectsUnderPrefix = objectStorageService
                .listS3ObjectsByPrefix(snapshotToRestore.getFolderPath())
                .stream().collect(Collectors.toMap(S3Object::key, object -> object));

        try{
            //BFS to traverse through the folder node and map each path to its parent folder
            Queue<FolderNode> queue = new LinkedList<>();
            queue.add(rootNodeOfSnapshot);

            Set<String> desiredSnapshotFolderPaths = new HashSet<>();

            while(!queue.isEmpty()){
                FolderNode currentFolderNode = queue.poll();
                String folderFullPath = currentFolderNode.getFullPath();
                desiredSnapshotFolderPaths.add(folderFullPath);

                //Determine the db parent folder
                UserFolder dbParentFolder = null;
                if(!currentFolderNode.getFullPath().equals(snapshotToRestore.getFolderPath())){
                    String parentFullPath = userFolderUtils.extractParentPath(currentFolderNode.getFullPath());
                    dbParentFolder = mapPathToSavedFolder.get(parentFullPath);
                    if(dbParentFolder == null){
                        throw new SnapshotRestoreException(String.format("Critical error: Parent folder not found in DB for path: %s", folderFullPath));
                    }

                }else{
                    //Get the parent of the root folder
                    dbParentFolder = userFolderBelongingToSnapshot.getParentFolder();
                }

                UserFolder dbFolder;
                if(activeDescendantUserFolders.containsKey(folderFullPath)){
                    //Update the existing folder
                    dbFolder = activeDescendantUserFolders.get(folderFullPath);
                    dbFolder.setParentFolder(dbParentFolder);
                    dbFolder.setUpdatedAt(updatedAt);
                    dbFolder.setIsDeleted(false);
                }else{
                    dbFolder = UserFolder.builder()
                            .name(userFolderUtils.extractFolderName(folderFullPath))
                            .folderPath(folderFullPath)
                            .isRoot(folderFullPath.equals("/" + userId))
                            .parentFolder(dbParentFolder)
                            .isDeleted(false)
                            .user(user)
                            .createdAt(updatedAt)
                            .updatedAt(updatedAt)
                            .build();
                }

                userFolderRepository.save(dbFolder);
                mapPathToSavedFolder.put(folderFullPath, dbFolder);

                //This only processes folder children
                for(FolderNode childNode: currentFolderNode.getChildren().values()){
                    //If the node has children, this is a folder
                    if(folderFullPath.endsWith("/")){
                        queue.add(childNode);
                    }
                }
            }
            log.info("{} folders processed and marked to be saved to the DB.", desiredSnapshotFolderPaths.size());


            Set<String> desiredFileSnapshotPaths = new HashSet<>();


            for(UserFolderSnapshotFileEntry fileEntry: entriesToRestore){
                String filePath = fileEntry.getS3Key();
                desiredFileSnapshotPaths.add(filePath);

                String parentPath = userFolderUtils.extractParentPath(filePath);
                UserFolder dbParentFolder = mapPathToSavedFolder.get(parentPath);

                if (dbParentFolder == null) {
                    throw new SnapshotRestoreException(String.format("Critical error: Parent folder not found for file path: %s", filePath));
                }

                UserFile dbFile;
                if(activeDescendantUserFiles.containsKey(filePath)){
                    dbFile = activeDescendantUserFiles.get(filePath);
                    dbFile.setUpdatedAt(updatedAt);
                    dbFile.setUserFolder(dbParentFolder);
                    dbFile.setIsDeleted(false);
                }else{
                    dbFile = UserFile
                            .builder()
                            .fileName(userFileUtils.extractFileName(filePath)) // Extract name from fullPath
                            .filePath(filePath)
                            .user(user)
                            .isDeleted(false)
                            .createdAt(updatedAt)
                            .updatedAt(updatedAt)
                            .userFolder(dbParentFolder)
                            .build();
                }

                dbFile.setCurrentVersion(restoredObjects.get(filePath));
                S3Object s3Object = allRestoredObjectsUnderPrefix.get(filePath);

                if(s3Object != null){
                    dbFile.setFileSize(s3Object.size());
                }else{
                    log.warn("S3 Object not found for file: {}", filePath);
                    dbFile.setFileSize(0L);
                }
                userFileRepository.save(dbFile);
            }
            log.info("{} files processed and marked to be saved to the DB.", desiredFileSnapshotPaths.size());

            //Soft delete files no longer in the snapshot
            for(UserFile oldFile: activeDescendantUserFiles.values()){
                if(!desiredFileSnapshotPaths.contains(oldFile.getFilePath())){
                    oldFile.setDeletedAt(updatedAt);
                    oldFile.setIsDeleted(true);

                    //Get the file type from deleted objects else query aws for it
                    if(deletedObjects.containsKey(oldFile.getFilePath())){
                        oldFile.setCurrentVersion(deletedObjects.get(oldFile.getFilePath()));
                    }else{
                        oldFile.setCurrentVersion(objectQueryService.getObjectVersionId(oldFile.getFilePath()));
                    }
                    userFileRepository.save(oldFile);
                }
            }

            List<UserFolder> foldersToDelete = new ArrayList<>(activeDescendantUserFolders.values());
            //Reverse the folders to avoid deleting parents before children
            foldersToDelete.sort(Comparator.comparing(UserFolder::getFolderPath).reversed());

            for(UserFolder oldFolder: foldersToDelete){
                if(!desiredSnapshotFolderPaths.contains(oldFolder.getFolderPath())){
                    log.info("Soft-deleting old folder from DB: {}", oldFolder.getFolderPath());
                    userFolderDeleteService.softDeleteFolder(userId, oldFolder.getId());
                }
            }

        }catch (DataIntegrityViolationException e){
            log.error("Failed to restore snapshot: {} for user: {} due to an integrity exception.", snapshotToRestore.getFolderSnapShotVersionId(), user.getUsername());
            rollBackSnapshotRestore(snapshotToRestore, deletedObjects, restoredObjects);
            throw new SnapshotRestoreException(String.format("Failed to restore snapshot: %s for user: %s due to an integrity exception.", snapshotToRestore.getFolderSnapShotVersionId(), user.getUsername()));
        }catch (Exception e){
            log.error("An error occurred while trying to restore snapshot: {} for user: {}", snapshotToRestore.getFolderSnapShotVersionId(), user.getUsername());
            rollBackSnapshotRestore(snapshotToRestore, deletedObjects, restoredObjects);
            throw new SnapshotRestoreException(String.format("An error occurred while trying to restore snapshot: %s for user: %s", snapshotToRestore.getFolderSnapShotVersionId(), user.getUsername()));

        }
    }

    @Override
    public void rollBackSnapshotRestore(UserFolderSnapshot snapshot, Map<String, String> objectsToRollback, Map<String, String> mapRestoredObjectKeysToVersionId){
        byte maxRetries = 5;
        short retryTimeInMillis = 10000;
        //Pointer maps that will be updated dynamically in the for loop
        Map<String, String> objectsToProcessForDelete = new HashMap<>(objectsToRollback);
        Map<String, String> objectsToProcessForRestore = new HashMap<>(mapRestoredObjectKeysToVersionId);

        log.info("Starting snapshot restore rollback for snapshot: {}", snapshot.getFolderSnapShotVersionId());

        for(int attempt = 0; attempt < maxRetries; attempt++) {
            //Keeps track of the objects that failed to restore/delete
            Map<String, String> currentAttemptFailedToDelete = new HashMap<>();
            Map<String, String> currentAttemptFailedToRestore = new HashMap<>();

            //Permanently delete the restored objects
            log.info("Attempting to delete previously restored objects.");

            for (Map.Entry<String, String> entrySet : objectsToProcessForDelete.entrySet()) {
                String s3Key = entrySet.getKey();
                String s3VersionId = entrySet.getValue();

                //Ensure the current object hasn't been deleted before trying to delete it
                    try{
                        objectDeleteService.permanentDeleteObjectVersion(s3Key, s3VersionId);
                    }catch (Exception e){
                        log.error("Failed to delete restored object: {} (VERSION: {})", s3Key, s3VersionId);
                        currentAttemptFailedToDelete.put(s3Key, s3VersionId);
                    }
            }

            if(currentAttemptFailedToDelete.isEmpty()){
                log.info("Successfully deleted previously restored objects. Size: {}", objectsToProcessForDelete.size());
            }else{
                log.error("Failed to permanently delete some previously restored objects. Objects remaining: {}", currentAttemptFailedToDelete.size());
            }

                log.info("Attempting to restore previously deleted objects.");
                for (Map.Entry<String, String> entrySet : objectsToProcessForRestore.entrySet()) {
                    String s3Key = entrySet.getKey();
                    String s3deleteMarkerId = entrySet.getValue();

                    //Undelete the deleted object by deleting the delete marker
                        try {
                            objectDeleteService.permanentDeleteObjectVersion(s3Key, s3deleteMarkerId);
                        }catch (Exception e){
                            log.error("Failed to restore deleted object: {} (VERSION: {})", s3Key, s3deleteMarkerId);
                            currentAttemptFailedToRestore.put(s3Key, s3deleteMarkerId);
                        }

                }

                if(currentAttemptFailedToRestore.isEmpty()){
                    log.info("Successfully rolled back previously deleted objects. Size: {}", objectsToProcessForRestore.size());
                }else{
                    log.error("Failed to rollback some previously deleted objects. Objects remaining: {}", currentAttemptFailedToRestore.size());
                }

                //Check if no objects failed to delete
                if(currentAttemptFailedToDelete.isEmpty() && currentAttemptFailedToRestore.isEmpty()){
                    log.info("Successfully rolled back restore operation for snapshot: {}", snapshot.getFolderSnapShotVersionId());
                    return;
                }

                objectsToProcessForDelete = currentAttemptFailedToDelete;
                objectsToProcessForRestore = currentAttemptFailedToRestore;

                if(attempt < maxRetries - 1 && !currentAttemptFailedToRestore.isEmpty() && !currentAttemptFailedToDelete.isEmpty()){
                    log.warn("Rollback attempt {} failed. Retrying in {} ms...", attempt, retryTimeInMillis);
                    retryRollBack(retryTimeInMillis);
                }else{
                    log.error("Last rollback attempt ({}) failed for snapshot: {}. No more retries will be performed.",
                            attempt + 1, snapshot.getFolderSnapShotVersionId());
                }
        }

        throw new SnapshotRestoreException(String.format("CRITICAL: Failed to rollback snapshot restore operation for snapshot: %s", snapshot.getFolderSnapShotVersionId()));
    }


    private void retryRollBack(short retryTimeInMillis){
        try{
            Thread.sleep(retryTimeInMillis);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}




