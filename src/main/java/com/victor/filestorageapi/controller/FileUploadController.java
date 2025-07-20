package com.victor.filestorageapi.controller;

import com.victor.filestorageapi.models.entities.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/files")
public class FileUploadController {

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam MultipartFile fileToUpload
            ){
        String fileName = fileToUpload.getName();
        Long fileSize = fileToUpload.getSize();
        String contentType = fileToUpload.getContentType();

    }
}
