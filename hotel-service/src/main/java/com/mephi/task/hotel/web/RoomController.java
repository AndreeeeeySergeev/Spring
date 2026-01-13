package com.mephi.task.hotel.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mephi.task.hotel.domain.Hotel;
import com.mephi.task.hotel.domain.Room;
import com.mephi.task.hotel.repo.HotelRepository;
import com.mephi.task.hotel.repo.RoomHoldRepository;
import com.mephi.task.hotel.repo.RoomRepository;
import com.mephi.task.hotel.service.AvailabilityService;
import com.mephi.task.hotel.web.dto.AvailabilityRequest;
import com.mephi.task.hotel.web.dto.RoomDto;
import com.mephi.task.hotel.web.dto.RoomStatsDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "Room management and availability APIs")
public class RoomController {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final AvailabilityService availabilityService;
    private final RoomHoldRepository roomHoldRepository;

    @GetMapping
    @Operation(summary = "Get available rooms", description = "Returns a filtered and sorted list of available rooms for a date range")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of rooms")
    public List<Room> listFree(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String direction) {
        return availabilityService.listAvailableRoomsFiltered(start, end, hotelId, available, sortBy, direction);
    }

    @GetMapping("/recommend")
    @Operation(summary = "Get recommended rooms", description = "Returns recommended available rooms for a date range (sorted by times booked)")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved recommended rooms")
    public List<Room> recommend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return availabilityService.listRecommendedRooms(start, end);
    }

    // Internal endpoint for inter-service calls (no auth required)
    @GetMapping("/internal/recommend")
    public List<Room> recommendInternal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return availabilityService.listRecommendedRooms(start, end);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a room", description = "Creates a new room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Room successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid room data")
    })
    public Room create(@Valid @RequestBody RoomDto dto) {
        Hotel hotel = hotelRepository.findById(dto.getHotelId()).orElseThrow();
        Room r = new Room();
        r.setHotel(hotel);
        r.setNumber(dto.getNumber());
        r.setAvailable(dto.isAvailable());
        r.setTimesBooked(dto.getTimesBooked());
        return roomRepository.save(r);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a room", description = "Completely replaces a room by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room successfully replaced"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public Room replace(@PathVariable Long id, @Valid @RequestBody RoomDto dto) {
        Room r = roomRepository.findById(id).orElseThrow();
        Hotel hotel = hotelRepository.findById(dto.getHotelId()).orElseThrow();
        r.setHotel(hotel);
        r.setNumber(dto.getNumber());
        r.setAvailable(dto.isAvailable());
        r.setTimesBooked(dto.getTimesBooked());
        return roomRepository.save(r);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a room", description = "Partially updates a room by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room successfully updated"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public Room update(@PathVariable Long id, @RequestBody RoomDto dto) {
        Room r = roomRepository.findById(id).orElseThrow();
        if (dto.getHotelId() != null) {
            r.setHotel(hotelRepository.findById(dto.getHotelId()).orElseThrow());
        }
        if (dto.getNumber() != null) r.setNumber(dto.getNumber());
        r.setAvailable(dto.isAvailable());
        if (dto.getTimesBooked() != null) r.setTimesBooked(dto.getTimesBooked());
        return roomRepository.save(r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a room", description = "Deletes a room by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Room successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public void delete(@PathVariable Long id) {
        roomRepository.deleteById(id);
    }

    @PostMapping("/{id}/confirm-availability")
    @Operation(summary = "Confirm room availability", description = "Confirms room availability for a booking (internal endpoint)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Availability confirmed"),
            @ApiResponse(responseCode = "409", description = "Conflict - room not available")
    })
    public ResponseEntity<Void> confirm(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityRequest req) {
        boolean ok = availabilityService.confirmAvailability(id, req.getStartDate(), req.getEndDate(), req.getRequestId(), req.getBookingId());
        return ok ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @GetMapping("/stats")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @Operation(summary = "Get room statistics", description = "Returns statistics about all rooms including active holds")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics")
    public List<RoomStatsDto> stats() {
        return roomRepository.findAll().stream().map(r -> {
            RoomStatsDto dto = new RoomStatsDto();
            dto.setRoomId(r.getId());
            dto.setRoomNumber(r.getNumber());
            if (r.getHotel() != null) {
                dto.setHotelId(r.getHotel().getId());
                dto.setHotelName(r.getHotel().getName());
            }
            dto.setTimesBooked(r.getTimesBooked());
            long holds = roomHoldRepository.countByRoomAndEndDateGreaterThanEqualAndStartDateLessThanEqual(r, java.time.LocalDate.now(), java.time.LocalDate.now());
            dto.setActiveHolds(holds);
            return dto;
        }).toList();
    }
}


