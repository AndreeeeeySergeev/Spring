package com.mephi.task.booking;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.mephi.task.booking.domain.User;
import com.mephi.task.booking.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class BookingDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("admin"));
        admin.setRole("ADMIN");
        userRepository.save(admin);

        User user = new User();
        user.setUsername("user");
        user.setPassword(encoder.encode("user"));
        user.setRole("USER");
        userRepository.save(user);
    }
}





