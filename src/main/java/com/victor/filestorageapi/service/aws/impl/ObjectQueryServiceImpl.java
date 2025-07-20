package com.victor.filestorageapi.service.aws.impl;

import com.victor.filestorageapi.exception.MyCustomS3Exception;
import com.victor.filestorageapi.service.aws.ObjectQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectQueryServiceImpl implements ObjectQueryService {

    private final S3Client s3Client;
    private final String bucketName = "my-local-bucket";

    public String getObjectVersionId(String key){
        try{
            GetObjectRequest getObjectRequest = GetObjectRequest
                    .builder()
                    .key(key)
                    .bucket(bucketName)
                    .build();

            GetObjectResponse getObjectResponse = s3Client.getObject(getObjectRequest).response();
            return getObjectResponse.versionId();
        }catch (S3Exception e){
            log.error("An S3 exception occurred while trying to get version id for: {}", key);
            throw new MyCustomS3Exception(String.format("An S3 exception occurred while trying to get version id for: %s", key));
        }catch (Exception e){
            log.error("An unexpected exception occurred while trying to get version id for: {}", key);
            throw new MyCustomS3Exception(String.format("An unexpected exception occurred while trying to get version id for: %s", key));
        }
    }

    @Override
    public String getObjectContentType(String key){
        try{
            GetObjectRequest getObjectRequest = GetObjectRequest
                    .builder()
                    .key(key)
                    .bucket(bucketName)
                    .build();

            GetObjectResponse getObjectResponse = s3Client.getObject(getObjectRequest).response();
            return getObjectResponse.contentType();
        }catch (S3Exception e){
            log.error("An S3 exception occurred while trying to get content type for: {}", key);
            throw new MyCustomS3Exception(String.format("An S3 exception occurred while trying to get version id for: %s", key));
        }catch (Exception e){
            log.error("An unexpected exception occurred while trying to get content type for: {}", key);
            throw new MyCustomS3Exception(String.format("An unexpected exception occurred while trying to get version id for: %s", key));
        }
    }

}
