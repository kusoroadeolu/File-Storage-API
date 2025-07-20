package com.victor.filestorageapi.service.aws.impl;

import com.victor.filestorageapi.service.aws.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final S3Client s3Client;

    @Override
    public String uploadFile(User user, String s3Key) {
        return "";
    }
}
