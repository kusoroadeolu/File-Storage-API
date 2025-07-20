package com.victor.filestorageapi.service.aws.impl;

import com.victor.filestorageapi.exception.MyCustomS3Exception;
import com.victor.filestorageapi.models.constants.S3Constant;
import com.victor.filestorageapi.models.entities.User;
import com.victor.filestorageapi.service.aws.ObjectStorageService;
import com.victor.filestorageapi.service.aws.S3VersioningManager;
import com.victor.filestorageapi.service.utils.S3Utils;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("local")
@Slf4j
@RequiredArgsConstructor
public class LocalStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final S3Utils s3Utils;
    private final S3VersioningManager s3VersioningManager;


    @Override
    public void uploadFile(String prefix, MultipartFile file) {
        // Implementation for file upload will go here
    }

    /**
     * Creates an object (preferably a folder-like object with contentType "application/x-directory") for a user.
     *
     * @param user The user for whom the object is created.
     * @param newS3Key The s3 key of the object or folder.
     * @return The version ID of the newly created or updated object.
     * @throws MyCustomS3Exception If creation of the object fails due to an S3 error or any other unexpected exception.
     */
    @Override
    public String createObject(User user, String newS3Key) {
        try {
            log.info("Attempting to create object '{}' for user '{}'.",
                     newS3Key, user.getUsername());
            S3Waiter s3Waiter = s3Client.waiter();

            PutObjectRequest putNewObjectRequest = PutObjectRequest
                    .builder()
                    .metadata(addCredentials(user))
                    .bucket(S3Constant.bucket_name)
                    .key(newS3Key)
                    .contentType("application/x-directory") // Assuming it's a folder-like object
                    .contentLength(0L)
                    .build();

            PutObjectResponse putResponse = s3Client.putObject(putNewObjectRequest, RequestBody.empty());
            String versionId = putResponse.versionId(); // Capture the version ID

            log.info("Successfully initiated put for object '{}' (Version ID: {}) for user '{}'.", newS3Key, versionId, user.getUsername());

            HeadObjectRequest headObjectRequest = HeadObjectRequest
                    .builder()
                    .bucket(S3Constant.bucket_name) // Specify bucket for head request
                    .key(newS3Key)
                    .versionId(versionId) // Wait for the specific version to exist
                    .build();

            log.debug("Waiting for object '{}' (Version ID: {}) to be created.", newS3Key, versionId);
            WaiterResponse<HeadObjectResponse> waiterResponse = s3Waiter.waitUntilObjectExists(headObjectRequest);

            waiterResponse.matched().response().ifPresent(headObjectResponse -> {
                log.debug("Waiter confirmed the existence of object: '{}' (Version ID: {}).", newS3Key, versionId);
            });

            log.info("Successfully created object '{}' (Version ID: {}) for user '{}'.", newS3Key, versionId, user.getUsername());
            return versionId;

        } catch (S3Exception e) {
            log.error("An S3 error occurred while trying to create object '{}' for user '{}' in bucket '{}'. Error: {}",
                    newS3Key, user.getUsername(), S3Constant.bucket_name, e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("An S3 error occurred while trying to create object '%s' for user '%s' in bucket '%s'.", newS3Key, user.getUsername(), S3Constant.bucket_name), e
            );
        } catch (Exception e) {
            log.error("An unexpected error occurred while trying to create object '{}' for user '{}'. Error: {}",
                    newS3Key, user.getUsername(), e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("An unexpected error occurred while trying to create object '%s' for user '%s'.", newS3Key, user.getUsername()), e
            );
        }
    }




    @Override
    public void deleteFile(String path) {
        // Implementation for file deletion will go here
    }

    @Override
    public Resource loadFile(String path) {
        // Implementation for loading files will go here
        return null;
    }



    private String performS3Copy(String newKey, String oldKey, String contentType, @Nullable String versionId){
        log.info("Attempting to move object from '{}' to '{}'.", oldKey, newKey);
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/x-directory";
        }
        S3Waiter s3Waiter = s3Client.waiter();
        String newObjectVersionId = null;

        try {
            CopyObjectRequest copyObjectRequest = CopyObjectRequest
                    .builder()
                    .sourceBucket(S3Constant.bucket_name)
                    .destinationBucket(S3Constant.bucket_name)
                    .sourceKey(oldKey)
                    .sourceVersionId(versionId)
                    .destinationKey(newKey)
                    .contentType(contentType)
                    .build();

            CopyObjectResponse copyResponse = s3Client.copyObject(copyObjectRequest);
            newObjectVersionId = copyResponse.versionId();

            HeadObjectRequest headNewObjectRequest = HeadObjectRequest
                    .builder()
                    .bucket(S3Constant.bucket_name)
                    .key(newKey)
                    .versionId(newObjectVersionId)
                    .build();


            log.debug("Waiting for new object '{}' (Version ID: '{}') to exist after copy.", newKey, newObjectVersionId);
            WaiterResponse<HeadObjectResponse> waiterResponse =
                    s3Waiter.waitUntilObjectExists(headNewObjectRequest);


            String finalNewObjectVersionId = newObjectVersionId;
            waiterResponse.matched().response().ifPresent(headObjectResponse -> {
                log.debug("Waiter confirmed the existence of moved object: '{}' (Version ID: '{}').", newKey, finalNewObjectVersionId);
            });

            log.info("Successfully copied object from '{}' to '{}' (New Object Version ID: '{}').", oldKey, newKey, newObjectVersionId);
            return newObjectVersionId;
        } catch (NoSuchKeyException e) {
            log.warn("Failed to move object: Source object '{}' was not found in bucket '{}'. Error: {}", oldKey, S3Constant.bucket_name, e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("The source object '%s' was not found in bucket '%s'.", oldKey, S3Constant.bucket_name), e
            );
        } catch (S3Exception e) {
            log.error("An S3 error occurred while trying to move object from '{}' to '{}' in bucket '{}'. Error: {}", oldKey, newKey, S3Constant.bucket_name, e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("An S3 error occurred while trying to move object from '%s' to '%s'.", oldKey, newKey), e
            );
        } catch (Exception e) {
            log.error("An unexpected error occurred while trying to move object from '{}' to '{}'. Error: {}", oldKey, newKey, e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("An unexpected error occurred while trying to move object from '%s' to '%s'.", oldKey, newKey), e
            );
        }
    }


    @Override
    public String copyS3ObjectVersion(String newKey, String oldKey, String versionId, String contentType){
        return performS3Copy(newKey, oldKey, contentType, versionId);
    }


    @Override
    public String retrieveVersionedObject(String s3ObjectKey, String versionForRetrieval){
        log.info("Attempting to retrieve object: {} (VERSION: {})", s3ObjectKey, versionForRetrieval);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest
                    .builder()
                    .bucket(S3Constant.bucket_name)
                    .key(s3ObjectKey)
                    .versionId(versionForRetrieval)
                    .build();

            log.info("Getting content type for object: {}", s3ObjectKey);
//            GetObjectResponse objectResponse = s3Client.getObject(getObjectRequest).response();
//
//            String contentType = objectResponse.contentType();
            String contentType = "";

            log.info("Successfully retrieved object: {}, content type: {} ", s3ObjectKey, contentType);

            return copyS3ObjectVersion(s3ObjectKey, s3ObjectKey, contentType, versionForRetrieval);

        }catch (NoSuchKeyException e){
            log.error("Failed to retrieve object: {} (VERSION: {}) because it does not exist", s3ObjectKey, versionForRetrieval);
            throw new MyCustomS3Exception(String.format("Failed to retrieve object: %s (VERSION: %s) because it does not exist", s3ObjectKey, versionForRetrieval), e);
        }catch (S3Exception e){
            log.error("An S3 error occurred while trying to retrieve object: {} (VERSION: {})", s3ObjectKey, versionForRetrieval);
            throw new MyCustomS3Exception(String.format("An S3 error occurred while trying to restore object: %s (VERSION: %s)", s3ObjectKey, versionForRetrieval), e);
        }catch (Exception e){
            log.error("An unexpected error occurred while trying to retrieve object: {} (VERSION: {})", s3ObjectKey, versionForRetrieval);
            throw new MyCustomS3Exception(String.format("An unexpected error occurred while trying to retrieve object: %s (VERSION: %s)", s3ObjectKey, versionForRetrieval), e);
        }
    }

    @Override
    public List<S3Object> listS3ObjectsByPrefix(String prefix){
        log.info("Attempting to list S3 objects for prefix: {}", prefix);
        List<S3Object> allObjects = new ArrayList<>();
        String continuationToken = null;
        short pageCount = 0;

        try{
            do{
                pageCount++;
                ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request
                        .builder()
                        .bucket(S3Constant.bucket_name)
                        .prefix(prefix);

                //Get the continuation token to get the next page of objects
                if(continuationToken != null){
                    requestBuilder.continuationToken(continuationToken);
                }

                ListObjectsV2Request listObjectsV2Request = requestBuilder.build();
                log.info("(Page: {}) Fetching S3 objects for prefix: {}. Continuation token: {}", pageCount, prefix, continuationToken);

                ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);

                allObjects.addAll(listObjectsV2Response.contents());

                if(listObjectsV2Response.isTruncated()){
                    continuationToken = listObjectsV2Response.nextContinuationToken();
                    log.info("More S3 objects found for prefix: {}. Continuation token: {}", prefix, continuationToken);
                }else{
                    continuationToken = null;
                    log.info("Finished listing all S3 objects for prefix: {}.", prefix);
                }

            }while (continuationToken != null);

            return allObjects;

        }catch (NoSuchKeyException e) {
            log.warn("Failed to list object: Source object '{}' was not found in bucket '{}'. Error: {}", prefix, S3Constant.bucket_name, e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("The source object '%s' was not found in bucket '%s'.", prefix , S3Constant.bucket_name), e
            );
        } catch (S3Exception e) {
            log.error("An S3 error occurred while trying to list an object from '{}' in bucket '{}'. Error: {}", prefix , S3Constant.bucket_name, e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("An S3 error occurred while trying to list objects from '%s'", prefix), e
            );
        } catch (Exception e) {
            log.error("An unexpected error occurred while trying to list objects from '{}'. Error: {}", prefix, e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("An unexpected error occurred while trying to list objects from '%s'.", prefix), e
            );
        }
    }


    private Map<String, String> addCredentials(User user) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("userId", user.getId().toString());
        credentials.put("owner", user.getUsername());
        return credentials;
    }
}