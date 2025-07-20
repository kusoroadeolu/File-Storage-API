package com.victor.filestorageapi.service.aws;

import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import java.util.List;
import java.util.Map;

public interface ObjectDeleteService {
    String softDeleteObject(String s3key);
    void permanentDeleteObjectVersion(String s3Key, String s3VersionId);
    Map<String, String> putDeleteMarkersOnAllObjectsUnderPrefix(String s3Key);
}
