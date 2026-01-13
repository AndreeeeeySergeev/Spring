package com.mephi.task.booking.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mephi.task.booking.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}


