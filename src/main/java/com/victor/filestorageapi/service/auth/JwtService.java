package com.victor.filestorageapi.service.auth;

import com.victor.filestorageapi.models.entities.UserPrincipal;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;

@Service
public interface JwtService {
    Key generateKey(String secret);
    String generateToken(UserPrincipal userPrincipal);
    Claims extractAllClaims(String token);
    String extractUsername(String token);
    Boolean isTokenExpired(String token);
    Boolean validateToken(String token, UserDetails userDetails);
}
