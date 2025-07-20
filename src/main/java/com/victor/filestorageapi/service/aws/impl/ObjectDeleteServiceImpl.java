package com.victor.filestorageapi.service.aws.impl;

import com.victor.filestorageapi.exception.MyCustomS3Exception;
import com.victor.filestorageapi.models.constants.S3Constant;
import com.victor.filestorageapi.service.aws.ObjectDeleteService;
import com.victor.filestorageapi.service.aws.S3VersioningManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectDeleteServiceImpl implements ObjectDeleteService {


    private final S3Client s3Client;

    /**
     * "Soft deletes" an object by creating a delete marker for its latest version.
     * The actual previous versions are retained.
     *
     * @param objectKey The full key (path) of the object to be soft-deleted.
     * @return The version ID of the delete marker that was created.
     * @throws MyCustomS3Exception If an S3Exception occurs or any other unexpected exception.
     */
    @Override
    public String softDeleteObject(String objectKey) {
        log.info("Attempting to soft-delete object with key: '{}'.", objectKey);
        S3Waiter s3Waiter = s3Client.waiter();

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest
                    .builder()
                    .bucket(S3Constant.bucket_name)
                    .key(objectKey)
                    .build();

            DeleteObjectResponse deleteResponse = s3Client.deleteObject(deleteObjectRequest);
            String deleteMarkerVersionId = deleteResponse.versionId(); // This is the version ID of the delete marker

            log.info("Successfully created delete marker for object '{}' (Delete Marker Version ID: '{}') in bucket '{}'.", objectKey, deleteMarkerVersionId, S3Constant.bucket_name);

            // It's good practice to wait for the object to "disappear" (i.e., the delete marker to be current)
            HeadObjectRequest headObjectRequest = HeadObjectRequest
                    .builder()
                    .bucket(S3Constant.bucket_name)
                    .key(objectKey)
                    .build();

            log.debug("Waiting for object '{}' to reflect soft-deletion (Delete Marker ID: '{}').", objectKey, deleteMarkerVersionId);
            // waitUntilObjectNotExists will confirm the current view of the object is 'not found', due to the delete marker
            WaiterResponse<HeadObjectResponse> waiterResponse = s3Waiter.waitUntilObjectNotExists(headObjectRequest);
            waiterResponse.matched().response().ifPresent(headObjectResponse -> {
                log.debug("Waiter confirmed the object '{}' is now soft-deleted (not found by default GET).", objectKey);
            });

            log.info("Object '{}' (soft-deleted with Delete Marker ID: '{}') confirmed as no longer accessible by default GET.", objectKey, deleteMarkerVersionId);
            return deleteMarkerVersionId;

        } catch (NoSuchKeyException e) {
            log.warn("Attempted to soft-delete object '{}', but it was not found in bucket '{}'. No delete marker created.", objectKey, S3Constant.bucket_name, e);
            throw new MyCustomS3Exception(
                    String.format("The object '%s' was not found in bucket '%s'.", objectKey, S3Constant.bucket_name), e
            );
        } catch (S3Exception e) {
            log.error("An S3 error occurred while trying to soft-delete object '{}' in bucket '{}'. Error: {}", objectKey, S3Constant.bucket_name, e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("An S3 error occurred while trying to soft-delete object '%s' from bucket '%s'.", objectKey, S3Constant.bucket_name), e
            );
        } catch (Exception e) {
            log.error("An unexpected error occurred while trying to soft-delete object '{}' in bucket '{}'. Error: {}", objectKey, S3Constant.bucket_name, e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("An unexpected error occurred while trying to soft-delete object '%s' from bucket '%s'.", objectKey, S3Constant.bucket_name), e
            );
        }
    }

    /**
     * Permanently deletes a specific version of an S3 object (or a delete marker). This action is irreversible.
     *
     * @param objectKey The full key (path) of the object.
     * @param versionId The specific version ID to delete (e.g., an old object version or a delete marker's version ID).
     * @throws MyCustomS3Exception If an S3Exception occurs or any other unexpected exception.
     */
    @Override
    public void permanentDeleteObjectVersion(String objectKey, String versionId) {
        log.info("Attempting to permanently delete object key: '{}' with Version ID: '{}'.", objectKey, versionId);
        S3Waiter s3Waiter = s3Client.waiter();

        try {
            DeleteObjectRequest objectRequest = DeleteObjectRequest
                    .builder()
                    .bucket(S3Constant.bucket_name)
                    .key(objectKey)
                    .versionId(versionId) // Specify the exact version to delete
                    .build();

            s3Client.deleteObject(objectRequest);

            log.info("Successfully initiated permanent delete for object '{}' (Version ID: '{}').", objectKey, versionId);

            HeadObjectRequest headObjectRequest = HeadObjectRequest
                    .builder()
                    .bucket(S3Constant.bucket_name)
                    .key(objectKey)
                    .build();

            try {
                s3Waiter.waitUntilObjectNotExists(headObjectRequest);
                log.info("Waiter confirmed object '{}' is no longer present as the current version.", objectKey);
            } catch (Exception waiterEx) {
                log.debug("Object '{}' still exists as a current version after deleting version '{}'.", objectKey, versionId);
            }

            log.info("Permanently deleted object '{}' (Version ID: '{}') from bucket '{}'.", objectKey, versionId, S3Constant.bucket_name);

        } catch (NoSuchKeyException e) {
            log.warn("Attempted to permanently delete object '{}' with Version ID: '{}', but this version was not found in bucket '{}'.", objectKey, versionId, S3Constant.bucket_name, e);
            throw new MyCustomS3Exception(
                    String.format("The object '%s' with Version ID '%s' was not found in bucket '%s'.", objectKey, versionId, S3Constant.bucket_name), e
            );
        } catch (S3Exception e) {
            log.error("An S3 error occurred while trying to permanently delete object '{}' (Version ID: '{}') from bucket '{}'. Error: {}", objectKey, versionId, S3Constant.bucket_name, e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("An S3 error occurred while trying to permanently delete object '%s' (Version ID: '%s') from bucket '%s'.", objectKey, versionId, S3Constant.bucket_name), e
            );
        } catch (Exception e) {
            log.error("An unexpected error occurred while trying to permanently delete object '{}' (Version ID: '{}') from bucket '{}'. Error: {}", objectKey, versionId, S3Constant.bucket_name, e.getMessage(), e);
            throw new MyCustomS3Exception(
                    String.format("An unexpected error occurred while trying to permanently delete object '%s' (Version ID: '%s') from bucket '%s'.", objectKey, versionId, S3Constant.bucket_name), e
            );
        }
    }

    public Map<String, String> putDeleteMarkersOnAllObjectsUnderPrefix(String s3ObjectKey){
        int pageCount = 0;
        String continuationToken = null;
        Map<String, String> mapS3KeyToDeleteMarkerId = new HashMap<>();
        List<ObjectIdentifier> objectsToBatchDelete = new ArrayList<>();

        try{
            do {
                ListObjectsV2Request.Builder requestBuilder =
                        ListObjectsV2Request.builder()
                                .bucket(S3Constant.bucket_name)
                                .prefix(s3ObjectKey);

                if(continuationToken != null){
                    requestBuilder.continuationToken(continuationToken);
                }

                ListObjectsV2Response listObjectsV2Response =
                        s3Client.listObjectsV2(requestBuilder.build());

                for (S3Object s3Object: listObjectsV2Response.contents()){
                    String key = s3Object.key();
                    ObjectIdentifier objectIdentifier = ObjectIdentifier.builder().key(key).build();
                    objectsToBatchDelete.add(objectIdentifier);

                    if(objectsToBatchDelete.size() == S3Constant.batch_size){
                        processBatchSoftDelete(objectsToBatchDelete, mapS3KeyToDeleteMarkerId);
                        objectsToBatchDelete.clear();
                    }
                }

                continuationToken = listObjectsV2Response.nextContinuationToken();
            }while (continuationToken != null);

            if(objectsToBatchDelete.isEmpty()){
                processBatchSoftDelete(objectsToBatchDelete, mapS3KeyToDeleteMarkerId);
            }

            return mapS3KeyToDeleteMarkerId;
        }catch (S3Exception e){
            log.error("An S3 error occurred while trying to soft delete objects under prefix: {}", s3ObjectKey);
            throw new MyCustomS3Exception(String.format("An S3 error occurred while trying to soft delete objects under prefix: %s", s3ObjectKey), e);
        }catch (Exception e){
            log.error("An unexpected error occurred while trying to soft delete objects under prefix: {}", s3ObjectKey);
            throw new MyCustomS3Exception(String.format("An unexpected error occurred while trying to soft delete objects under prefix: %s", s3ObjectKey), e);
        }

    }

    private void processBatchSoftDelete(List<ObjectIdentifier> objectsToBatchDelete, Map<String, String> mapS3ObjectsToDeleteMarkerId) {
        if(objectsToBatchDelete.isEmpty())return;

        Delete delete = Delete
                .builder()
                .objects(objectsToBatchDelete)
                .build();

        DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest
                .builder()
                .bucket(S3Constant.bucket_name)
                .delete(delete)
                .build();

        try{
            DeleteObjectsResponse deleteObjectsResponse = s3Client.deleteObjects(deleteObjectsRequest);

            if(deleteObjectsResponse.hasDeleted()){
                for(DeletedObject deletedObject: deleteObjectsResponse.deleted()){
                    String key = deletedObject.key();
                    String deleteMarkerId = deletedObject.deleteMarkerVersionId();
                    if(deleteMarkerId != null){
                        mapS3ObjectsToDeleteMarkerId.put(key,deleteMarkerId);
                    }else{
                        mapS3ObjectsToDeleteMarkerId.put(key, null);
                    }
                }
            }

            if(deleteObjectsResponse.hasErrors()){
                deleteObjectsResponse.errors().forEach(
                        error -> log.error("Failed to delete object: {}. Code: {}. Message: {}", error.key(), error.code(), error.message()
                        ));
                throw new MyCustomS3Exception(String.format("Batch delete failed for some objects. Size: %s", deleteObjectsResponse.errors().size()));
            }

        } catch (Exception e){
            log.error("S3 batch delete operation failed. ");
            throw new MyCustomS3Exception("S3 batch delete operation failed", e);
        }


    }

}

