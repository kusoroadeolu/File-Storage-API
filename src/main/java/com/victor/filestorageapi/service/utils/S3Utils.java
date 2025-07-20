package com.victor.filestorageapi.service.utils;

import com.victor.filestorageapi.exception.MyCustomS3Exception;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.utils.Validate;

@Component
@Slf4j
@RequiredArgsConstructor
public class S3Utils {
    /**
     *
     * */
    public void initializeBucket(String bucketName, String region ,S3Client s3Client) {
        log.info("Attempting to create S3 bucket: {}", bucketName);
        S3Waiter s3Waiter = s3Client.waiter();

        try{
            if(!doesBucketExist(bucketName, s3Client)){
                log.info("Bucket: {} does not exist. Initializing creation.", bucketName);
                CreateBucketRequest bucketRequest = CreateBucketRequest
                        .builder()
                        .bucket(bucketName)
                        .createBucketConfiguration(CreateBucketConfiguration
                                .builder()
                                .locationConstraint(region)
                                .build())
                        .build();
                s3Client.createBucket(bucketRequest);

                HeadBucketRequest bucketRequestWait = HeadBucketRequest
                        .builder()
                        .bucket(bucketName)
                        .build();

                log.info("Waiting for bucket: {} to be created", bucketName);
                WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
                waiterResponse.matched().response().ifPresent(headBucketResponse -> {
                    log.debug("Waiter confirmed bucket {} now exists. Response {}", bucketName, headBucketResponse.sdkHttpResponse());
                });

                log.info("Successfully created S3 bucket: {}", bucketName);
            }else{
                log.info("S3 bucket {} already exists.", bucketName);
            }
        }catch (S3Exception se){
            log.error("An S3 error occurred while trying to create the bucket: {}", bucketName, se);
            throw new MyCustomS3Exception("Failed to initialize S3 bucket " + bucketName, se);
        }catch (Exception e){
            log.error("An unexpected error occurred while trying to create bucket: {}", bucketName);
            throw new MyCustomS3Exception("An unexpected error occurred while trying to create bucket: " + bucketName, e);
        }
    }

    public boolean doesBucketExist(String bucketName, S3Client s3Client){
        try{
            Validate.notEmpty(bucketName, "Bucket name must not be empty");
            log.info("Checking for S3 bucket: {}", bucketName);
            HeadBucketRequest request = HeadBucketRequest
                    .builder()
                    .bucket(bucketName)
                    .build();
            s3Client.headBucket(request);
            log.info("Found S3 bucket: {}", bucketName);
            return true;
        }catch (S3Exception ase){
            log.error("Could not check for existence of bucket: {}", bucketName);
            throw new MyCustomS3Exception("Could not check for existence of bucket: " + bucketName);
        }catch (Exception e){
            log.error("An unexpected error occurred while trying to check for bucket: {}", bucketName);
            return false;
        }
    }



    public boolean doesObjectExist(String objectName, String bucketName, S3Client s3Client){
        try{
            Validate.notEmpty(objectName, "The object name must not be empty");
            log.info("Looking for object with name {} in bucket {}", objectName, bucketName);
            HeadObjectRequest request = HeadObjectRequest
                    .builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();
            s3Client.headObject(request);
            log.info("Found object with name {} in bucket {}, ", objectName, bucketName);
            return true;
        }catch (NoSuchKeyException noSuchKeyException){
            log.warn("No object with name {} in bucket {} was found: ", objectName, bucketName);
            return false;
        }catch (S3Exception s3Exception){
            log.error("Could not check for object existence in bucket: {}", bucketName);
            throw new MyCustomS3Exception("Could not check for object existence in bucket: " + bucketName);
        }catch (Exception e){
            log.error("An unexpected exception occurred while checking for object existence in bucket: {}", bucketName);
            return false;
        }
    }
}
