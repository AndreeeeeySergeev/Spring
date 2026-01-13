package com.mephi.task.hotel.repo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mephi.task.hotel.domain.Room;
import com.mephi.task.hotel.domain.RoomHold;

public interface RoomHoldRepository extends JpaRepository<RoomHold, Long> {
    List<RoomHold> findByRoomAndEndDateGreaterThanEqualAndStartDateLessThanEqual(Room room, LocalDate start, LocalDate end);
    Optional<RoomHold> findByRequestId(String requestId);
    long countByRoomAndEndDateGreaterThanEqualAndStartDateLessThanEqual(Room room, LocalDate start, LocalDate end);
}


