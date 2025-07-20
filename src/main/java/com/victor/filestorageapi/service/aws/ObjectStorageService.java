package com.victor.filestorageapi.service.aws;

import com.victor.filestorageapi.models.entities.User;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;

@Service
public interface ObjectStorageService {
    String  createObject(User user, String newS3Key);
    String copyS3ObjectVersion(String newKey, String oldKey, String versionId, String contentType);
    String retrieveVersionedObject(String prefix, String versionForRetrieval);
    List<S3Object> listS3ObjectsByPrefix(String prefix);

    void uploadFile(String prefix, MultipartFile file);
    void deleteFile(String path);
    Resource loadFile(String prefix);
}

