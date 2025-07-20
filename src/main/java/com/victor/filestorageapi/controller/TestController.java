package com.victor.filestorageapi.controller;

import com.victor.filestorageapi.models.entities.UserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/me")
    public String validateLogin(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return "Hello: " + userPrincipal.getUsername();
    }
}
