package com.mephi.task.booking.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mephi.task.booking.domain.User;
import com.mephi.task.booking.repo.BookingRepository;
import com.mephi.task.booking.repo.UserRepository;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @PostMapping
    public ResponseEntity<User> create(@RequestBody CreateUser req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(encoder.encode(req.getPassword()));
        u.setRole(req.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(userRepository.save(u));
    }

    @PatchMapping
    public ResponseEntity<User> update(@RequestBody UpdateUser req) {
        return userRepository.findById(req.getId())
                .map(u -> {
                    if (req.getUsername() != null) u.setUsername(req.getUsername());
                    if (req.getPassword() != null) u.setPassword(encoder.encode(req.getPassword()));
                    if (req.getRole() != null) u.setRole(req.getRole());
                    return ResponseEntity.ok(userRepository.save(u));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestBody DeleteUser req) {
        if (!userRepository.existsById(req.getId())) {
            return ResponseEntity.notFound().build();
        }
        // Delete user's bookings first (foreign key constraint)
        bookingRepository.findAll().stream()
                .filter(b -> b.getUser().getId().equals(req.getId()))
                .forEach(b -> bookingRepository.deleteById(b.getId()));
        
        userRepository.deleteById(req.getId());
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CreateUser {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        @NotBlank
        private String role;
    }

    @Data
    public static class UpdateUser {
        private Long id;
        private String username;
        private String password;
        private String role;
    }

    @Data
    public static class DeleteUser {
        private Long id;
    }
}


