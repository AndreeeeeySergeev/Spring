package com.mephi.task.booking.service;

import com.mephi.task.booking.domain.User;
import com.mephi.task.booking.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional
    public User register(String username, String rawPassword, String role) {
        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            return existing.get();
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(rawPassword));
        u.setRole(role);
        return userRepository.save(u);
    }
}


