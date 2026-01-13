package com.mephi.task.booking.security;

import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

    @Value("${security.jwt.secret:}")
    private String secret;

    private Key key;

    @PostConstruct
    public void init() {
        if (secret != null && !secret.isBlank()) {
            byte[] candidate;
            try {
                candidate = Base64.getDecoder().decode(secret);
            } catch (IllegalArgumentException e) {
                candidate = secret.getBytes();
            }
            this.key = Keys.hmacShaKeyFor(candidate);
        } else {
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
    }

    public String generateToken(String username, String role, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Key getKey() {
        return key;
    }
}


