package com.mephi.task.booking.repo;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mephi.task.booking.domain.Booking;
import com.mephi.task.booking.domain.User;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    @Query("SELECT b FROM Booking b WHERE b.user = :user")
    Page<Booking> findByUser(@Param("user") User user, Pageable pageable);
    
    @Query("SELECT b FROM Booking b WHERE b.requestId = :requestId")
    Optional<Booking> findByRequestId(@Param("requestId") String requestId);
}


