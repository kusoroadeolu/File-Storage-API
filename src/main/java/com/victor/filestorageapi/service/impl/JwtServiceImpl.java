package com.victor.filestorageapi.service.impl;

import com.victor.filestorageapi.config.JwtProperties;
import com.victor.filestorageapi.exception.JwtAuthenticationException;
import com.victor.filestorageapi.models.UserPrincipal;
import com.victor.filestorageapi.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;

@RequiredArgsConstructor
@Service
public class JwtServiceImpl implements JwtService {

    private final JwtProperties properties;

    @Override
    public SecretKey generateKey(String secret) {
        byte[] keyBytes = Decoders.BASE64URL.decode(secret);
        return hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateToken(UserPrincipal userPrincipal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userPrincipal.getId());
        claims.put("userRoles", userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList());

        return Jwts.builder()
                .claims(claims)
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + properties.getExpiration()))
                .signWith(generateKey(properties.getSecret()))
                .compact();
    }

    @Override
    public Claims extractAllClaims(String token) {
        try{
        return Jwts.parser()
                .verifyWith(generateKey(properties.getSecret()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        }catch (Exception ex){
            throw new RuntimeException("Invalid or Malformed Jwt Token", ex);
        }
    }

    @Override
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Override
    public Boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    @Override
    public Boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}
