package com.mephi.task.hotel.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mephi.task.hotel.domain.Room;
import com.mephi.task.hotel.domain.RoomHold;
import com.mephi.task.hotel.repo.RoomHoldRepository;
import com.mephi.task.hotel.repo.RoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final RoomRepository roomRepository;
    private final RoomHoldRepository roomHoldRepository;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AvailabilityService.class);

    @Transactional(readOnly = true)
    public List<Room> listAvailableRooms(LocalDate start, LocalDate end) {
        List<Room> rooms = roomRepository.findAll();
        return rooms.stream()
                .filter(Room::isAvailable)
                .filter(r -> isFree(r, start, end))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Room> listAvailableRoomsFiltered(LocalDate start,
                                                 LocalDate end,
                                                 Long hotelId,
                                                 Boolean available,
                                                 String sortBy,
                                                 String direction) {
        List<Room> rooms = roomRepository.findAll();
        return rooms.stream()
                .filter(r -> available == null || r.isAvailable() == available)
                .filter(r -> hotelId == null || (r.getHotel() != null && r.getHotel().getId().equals(hotelId)))
                .filter(r -> isFree(r, start, end))
                .sorted(buildRoomComparator(sortBy, direction))
                .toList();
    }

    private java.util.Comparator<Room> buildRoomComparator(String sortBy, String direction) {
        java.util.Comparator<Room> cmp;
        if ("timesBooked".equalsIgnoreCase(sortBy)) {
            cmp = java.util.Comparator.comparingLong(Room::getTimesBooked);
        } else if ("number".equalsIgnoreCase(sortBy)) {
            cmp = java.util.Comparator.comparing(Room::getNumber, java.util.Comparator.nullsLast(String::compareTo));
        } else {
            cmp = java.util.Comparator.comparing(Room::getId);
        }
        if ("desc".equalsIgnoreCase(direction)) {
            cmp = cmp.reversed();
        }
        return cmp;
    }

    @Transactional(readOnly = true)
    public List<Room> listRecommendedRooms(LocalDate start, LocalDate end) {
        return listAvailableRooms(start, end).stream()
                .sorted(Comparator.comparingLong(Room::getTimesBooked).thenComparing(Room::getId))
                .toList();
    }

    @Transactional
    public boolean confirmAvailability(Long roomId, LocalDate start, LocalDate end, String requestId, String bookingId) {
        log.info("confirmAvailability requestId={} bookingId={} roomId={} start={} end={}", requestId, bookingId, roomId, start, end);
        Optional<RoomHold> existing = roomHoldRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            log.info("confirmAvailability idempotent hit requestId={} bookingId={} roomId={}", requestId, bookingId, roomId);
            return true; // idempotent
        }
        // Pessimistic lock to avoid concurrent confirmation on the same room
        Room room = Optional.ofNullable(roomRepository.findByIdForUpdate(roomId)).orElseThrow();
        if (!room.isAvailable() || !isFree(room, start, end)) {
            log.warn("confirmAvailability conflict bookingId={} roomId={} start={} end={}", bookingId, roomId, start, end);
            return false;
        }
        RoomHold hold = new RoomHold();
        hold.setRoom(room);
        hold.setStartDate(start);
        hold.setEndDate(end);
        hold.setRequestId(requestId);
        hold.setBookingId(bookingId);
        roomHoldRepository.save(hold);
        room.setTimesBooked(room.getTimesBooked() + 1);
        roomRepository.save(room);
        log.info("confirmAvailability success bookingId={} requestId={} holdCreated", bookingId, requestId);
        return true;
    }

    @Transactional
    public void releaseHold(Long roomId, String requestId) {
        roomHoldRepository.findByRequestId(requestId).ifPresent(hold -> {
            log.info("releaseHold requestId={} roomId={}", requestId, roomId);
            Room room = hold.getRoom();
            roomHoldRepository.delete(hold);
            if (room.getTimesBooked() > 0) {
                room.setTimesBooked(room.getTimesBooked() - 1);
                roomRepository.save(room);
            }
        });
    }

    private boolean isFree(Room room, LocalDate start, LocalDate end) {
        return roomHoldRepository
                .findByRoomAndEndDateGreaterThanEqualAndStartDateLessThanEqual(room, start, end)
                .isEmpty();
    }
}


