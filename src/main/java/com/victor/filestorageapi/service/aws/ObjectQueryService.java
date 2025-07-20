package com.victor.filestorageapi.service.aws;

public interface ObjectQueryService {
    public String getObjectVersionId(String key);

    String getObjectContentType(String key);

}
