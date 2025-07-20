package com.victor.filestorageapi.service.utils;

import com.victor.filestorageapi.models.FolderNode;
import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.models.entities.UserFolder;
import com.victor.filestorageapi.repository.UserFolderRepository;
import com.victor.filestorageapi.service.aws.ObjectDeleteService;
import com.victor.filestorageapi.service.aws.ObjectStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class UserFolderUtils {


    //Handles data integrity exceptions for folders
    public void handleDataIntegrityException(ObjectDeleteService objectDeleteService, DataIntegrityViolationException ex, String folderPath , String s3ObjectVersion, User user){
        if (s3ObjectVersion != null) {
            try {
                objectDeleteService.permanentDeleteObjectVersion(folderPath, s3ObjectVersion);
                log.info("Successfully compensated: Deleted S3 root object {} (Version: {}) for user {} after DB save failure.", user.getId(), s3ObjectVersion, user.getUsername());
            } catch (Exception cleanupEx) {
                log.error("CRITICAL: Failed to clean up S3 root object {} (Version: {}) for user {} after DB save failure. Manual intervention may be required.", user.getId(), s3ObjectVersion, user.getUsername(), cleanupEx);
            }
        }
    }

    public void handleGeneralException(ObjectDeleteService objectDeleteService, Exception ex, String folderPath ,String s3ObjectVersion, User user){
        if (s3ObjectVersion != null) {
            try {
                objectDeleteService.permanentDeleteObjectVersion(folderPath, s3ObjectVersion);
                log.info("Successfully compensated: Deleted root object {} (Version: {}) for user {} after DB save failure.", user.getId(), s3ObjectVersion, user.getUsername());
            } catch (Exception cleanupEx) {
                log.error("CRITICAL: Failed to clean up S3 root object {} (Version: {}) for user {} after DB save failure. Manual intervention may be required.", user.getId().toString(), s3ObjectVersion, user.getUsername(), cleanupEx);
            }
        }
    }

    public void handleDataAccessException(ObjectDeleteService objectDeleteService, DataAccessException e, String folderPath ,String deleteMarkerVersionId, User user) {
        if (deleteMarkerVersionId != null) {
            try {
                objectDeleteService.permanentDeleteObjectVersion(folderPath, deleteMarkerVersionId);
                log.info("Successfully compensated: Deleted S3 root object {} (Version: {}) for user {} after DB save failure.", user.getId(), deleteMarkerVersionId, user.getUsername());
            } catch (Exception cleanupEx) {
                log.error("CRITICAL: Failed to clean up S3 root object {} (Version: {}) for user {} after DB save failure. Manual intervention may be required.", user.getId(), deleteMarkerVersionId, user.getUsername(), cleanupEx);
            }
        }
    }

    public String extractFolderName(String folderPath){
        String cleanedPath;

        if(folderPath == null || folderPath.isEmpty()){
            folderPath = "/";
        }

        if(folderPath.length() > 1 && folderPath.endsWith("/")){
            cleanedPath = folderPath.substring(0, folderPath.length() - 1);
        }else{
            cleanedPath = folderPath;
        }

        int lastSlashIndex = cleanedPath.lastIndexOf("/");
        return cleanedPath.substring(lastSlashIndex + 1);
    }

    public String extractParentPath(String folderPath) {
        String cleanedPath;

        if(folderPath == null || folderPath.isEmpty()){
            folderPath = "/";
        }

        if(folderPath.length() > 1 && folderPath.endsWith("/")){
            cleanedPath = folderPath.substring(0, folderPath.length() - 1);
        }else{
            cleanedPath = folderPath;
        }

        int lastSlashIndex = cleanedPath.lastIndexOf("/");
        return cleanedPath.substring(0, lastSlashIndex);
    }




    public FolderNode buildFolderTree(String rootPath, List<String> folderPaths){
        FolderNode rootNode = new FolderNode(rootPath);

        for(String path: folderPaths){
            if(path == null || path.isEmpty())continue;


            String[] parts = path.split("/");
            List<String> validParts = Arrays
                    .stream(parts)
                    .filter(f -> !f.isEmpty())
                    .toList();
            FolderNode currentNode = rootNode;

            for (String part : validParts) {
                String childFullPath = currentNode.getFullPath() + part;
                childFullPath = childFullPath.endsWith("/") ? childFullPath : childFullPath + "/";

                currentNode = currentNode.addChild(part, childFullPath);
            }
        }
        return rootNode;
    }


}
