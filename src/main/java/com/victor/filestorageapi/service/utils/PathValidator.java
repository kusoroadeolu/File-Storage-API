package com.victor.filestorageapi.service.utils;

import org.springframework.stereotype.Component;

@Component
public class PathValidator {
    public String extractParentFolderPathFromKey(String newKey) {
        if(newKey == null || newKey.isEmpty()){
            return "";
        }

        int lastSlashIndex = newKey.lastIndexOf("/");
        return lastSlashIndex == -1 ? null : newKey.substring(lastSlashIndex, newKey.length() - 1);
    }

    public String buildS3Key(String newPrefix, String objectName){
        String fullPath = newPrefix.endsWith("/") ? newPrefix + objectName : newPrefix + "/" + objectName;
        return fullPath.endsWith("/") ? fullPath : fullPath + "/";
    }

    public String getContentTypeSafe(String path) {
        if (path.endsWith("/")) {
            return "application/x-directory";
        }

        int lastIndex = path.lastIndexOf(".");
        if (lastIndex > 0 && lastIndex < path.length() - 1) {
            return path.substring(lastIndex);
        }
        return "";
    }

    public String getKeyName(String key) {
        if (key == null || key.isEmpty()){
            return "";
        }

        String extractedName = null;

        if(key.startsWith("/")){
            extractedName = key.substring(1);
        }

        if(key.endsWith("/") && key.length() > 1){
            extractedName = key.substring(0, key.length() - 1);
        }else if(key.equals("/")){
            return "";
        }

        int lastIndexSlash = extractedName.lastIndexOf("/") ;
        String newName = extractedName.substring(lastIndexSlash);
        int lastIndexDot = newName.lastIndexOf(".");

        if(lastIndexDot > 0){
            return newName.substring(0, lastIndexDot);
        }else{
            return newName;
        }
    }
}
