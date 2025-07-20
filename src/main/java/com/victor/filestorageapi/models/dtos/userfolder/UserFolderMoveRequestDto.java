package com.victor.filestorageapi.models.dtos.userfolder;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserFolderMoveRequestDto(
        @NotNull(message = "User ID should not be null")
        UUID userId,
        @NotNull(message = "Folder ID should not be null")
        UUID folderId,
        @NotNull(message = "New Parent Folder ID should not be null")
        UUID newParentFolderId

) {
}
