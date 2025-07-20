package com.victor.filestorageapi.service.aws;

import org.springframework.stereotype.Service;

@Service
public interface S3VersioningManager {
    void enableVersioning();
    void disableVersioning();
    void configureVersioningLifeCycleRules();

}
