package com.mephi.task.hotel.security;

import java.security.Key;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
            this.key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        }
    }

    public Key getKey() {
        return key;
    }
}


