package com.victor.filestorageapi.models.dtos.userfolder;

public record UserFolderResponseDto(
        String folderName, String folderPath, boolean isRoot , String ownedBy
) {
}
