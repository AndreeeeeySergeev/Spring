package com.mephi.task.hotel.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mephi.task.hotel.domain.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
}


