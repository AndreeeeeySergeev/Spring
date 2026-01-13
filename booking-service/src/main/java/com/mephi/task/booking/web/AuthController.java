package com.mephi.task.booking.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mephi.task.booking.domain.User;
import com.mephi.task.booking.repo.UserRepository;
import com.mephi.task.booking.security.JwtService;
import com.mephi.task.booking.service.UserService;
import com.mephi.task.booking.web.dto.AuthDtos;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and authentication APIs")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<AuthDtos.TokenResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        User u = userService.register(req.getUsername(), req.getPassword(), "USER");
        String token = jwtService.generateToken(u.getUsername(), u.getRole(), 3600);
        AuthDtos.TokenResponse resp = new AuthDtos.TokenResponse();
        resp.setToken(token);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/auth")
    @Operation(summary = "Authenticate user", description = "Authenticates a user and returns JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthDtos.TokenResponse> auth(@Valid @RequestBody AuthDtos.AuthRequest req) {
        User u = userRepository.findByUsername(req.getUsername()).orElse(null);
        if (u == null || !encoder.matches(req.getPassword(), u.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtService.generateToken(u.getUsername(), u.getRole(), 3600);
        AuthDtos.TokenResponse resp = new AuthDtos.TokenResponse();
        resp.setToken(token);
        return ResponseEntity.ok(resp);
    }
}


