package com.victor.filestorageapi.service.aws;

import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

@Service
public interface FileUploadService {
    String uploadFile(User user, String s3Key);
}
