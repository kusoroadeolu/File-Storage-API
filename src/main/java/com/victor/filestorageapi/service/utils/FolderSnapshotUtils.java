package com.victor.filestorageapi.service.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class FolderSnapshotUtils {
    public String buildSnapshotVersion(LocalDateTime timestamp){
        if(timestamp == null){
            log.warn("Timestamp to build snapshot is null");
            return "";
        }
        return "SNAPSHOT_" + timestamp + "_" + UUID.randomUUID();
    }
}
