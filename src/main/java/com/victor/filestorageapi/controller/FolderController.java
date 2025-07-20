package com.victor.filestorageapi.controller;

import com.victor.filestorageapi.mapper.UserFolderMapper;
import com.victor.filestorageapi.models.dtos.userfolder.UserFolderMoveRequestDto;
import com.victor.filestorageapi.models.dtos.userfolder.UserFolderCreateRequestDto;
import com.victor.filestorageapi.models.dtos.userfolder.UserFolderResponseDto;
import com.victor.filestorageapi.models.entities.UserFolder;
import com.victor.filestorageapi.models.entities.UserPrincipal;
import com.victor.filestorageapi.service.folder.UserFolderCreationService;
import com.victor.filestorageapi.service.folder.UserFolderDeleteService;
import com.victor.filestorageapi.service.folder.UserFolderMoveService;
import com.victor.filestorageapi.service.snapshots.UserFolderSnapshotRestoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/folders")
public class FolderController {
    private final UserFolderCreationService userFolderCreationService;
    private final UserFolderDeleteService userFolderDeleteService;
    private final UserFolderMoveService userFolderMoveService;
    private final UserFolderSnapshotRestoreService userFolderSnapshotRestoreService;
    private final UserFolderMapper userFolderMapper;


    @PostMapping("/root")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserFolderResponseDto> createRootFolder(@AuthenticationPrincipal UserPrincipal userPrincipal){
        UserFolder userFolder = userFolderCreationService.createRootFolder(userPrincipal.getId());
        var response = userFolderMapper.mapUserFolderToResponse(userFolder);
        URI uri = URI.create("/api/v1/folders/" + userFolder.getName());
        return ResponseEntity.created(uri).body(response);
    }


    @PostMapping("/new")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserFolderResponseDto> createNewFolder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody(required = false) UserFolderCreateRequestDto requestDto
            ){

        UserFolder userFolder = userFolderCreationService.createSubFolder(userPrincipal.getId(),
                requestDto.parentFolderId(),
                requestDto.folderName());
        var response = userFolderMapper.mapUserFolderToResponse(userFolder);
        URI uri = URI.create("/api/v1/folders/" + userFolder.getName());
        return ResponseEntity.created(uri).body(response);
    }

    @DeleteMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<UserFolder> softDeleteFolder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String folderId
    ){
        UUID userId = userPrincipal.getId();
        userFolderDeleteService.softDeleteFolder(userId, UUID.fromString(folderId));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/move")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserFolderResponseDto> moveFolderAndChildren(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UserFolderMoveRequestDto requestDto
    ){
        userFolderMoveService.moveFolder(userPrincipal.getId(),
                requestDto.folderId(),
                requestDto.newParentFolderId());
        return ResponseEntity.ok().build();
    }
}
