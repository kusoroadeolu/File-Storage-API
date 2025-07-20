package com.victor.filestorageapi.models.dtos.userfolder;


import jakarta.validation.constraints.NotEmpty;

import java.util.UUID;

public record UserFolderCreateRequestDto(
        @NotEmpty(message = "Folder name must not be empty")
        String folderName,
        UUID parentFolderId) {
}
