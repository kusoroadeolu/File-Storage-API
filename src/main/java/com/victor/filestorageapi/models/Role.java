package com.victor.filestorageapi.models;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
public enum Role implements GrantedAuthority {
    USER("ROLE_USER");

    private final String authority;

    Role(String authority){
       this.authority = authority;
    }

}
