package com.victor.filestorageapi.service.utils;

import com.victor.filestorageapi.models.entities.UserFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UserFileUtils {
    public String extractFileName(String filePath){
        String cleanedPath;

        if(filePath == null || filePath.isEmpty()){
            filePath = "/";
        }

        if(filePath.length() > 1 && filePath.endsWith("/")){
            cleanedPath = filePath.substring(0, filePath.length() - 1);
        }else{
            cleanedPath = filePath;
        }

        int lastSlashIndex = cleanedPath.lastIndexOf("/");
        return cleanedPath.substring(lastSlashIndex + 1);
    }

}
