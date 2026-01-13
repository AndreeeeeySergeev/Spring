package com.mephi.task.booking.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mephi.task.booking.client.HotelClient;
import com.mephi.task.booking.client.dto.RoomDto;
import com.mephi.task.booking.domain.Booking;
import com.mephi.task.booking.domain.User;
import com.mephi.task.booking.repo.UserRepository;
import com.mephi.task.booking.service.BookingService;
import com.mephi.task.booking.web.dto.BookingDtos;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management APIs")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final HotelClient hotelClient;

    @PostMapping("/booking")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a booking", description = "Creates a new booking for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid booking data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Conflict - booking failed")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Booking> create(
            @Valid @RequestBody BookingDtos.CreateBookingRequest req,
            Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        if (!req.isAutoSelect() && req.getRoomId() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (req.getStartDate() == null || req.getEndDate() == null || !req.getStartDate().isBefore(req.getEndDate())) {
            return ResponseEntity.badRequest().build();
        }
        Long roomId = req.isAutoSelect() ? pickRoom(req.getStartDate(), req.getEndDate()) : req.getRoomId();
        String requestId = req.getRequestId();
        Booking pending = bookingService.createPending(user.getId(), roomId, req.getStartDate(), req.getEndDate(), requestId);
        try {
            Booking confirmed = bookingService.confirm(pending.getId());
            return ResponseEntity.ok(confirmed);
        } catch (Exception ex) {
            bookingService.cancelAndCompensate(pending.getId(), requestId);
            return ResponseEntity.status(409).build();
        }
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get user's bookings", description = "Returns paginated list of bookings for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public org.springframework.data.domain.Page<Booking> myBookings(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable,
            Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return bookingService.findForUser(user.getId(), pageable);
    }

    @GetMapping("/booking/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get booking by ID", description = "Returns a specific booking by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Booking> get(
            @PathVariable Long id, 
            Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        return bookingService.findForUser(user.getId()).stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/booking/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Cancel a booking", description = "Cancels a booking and releases the room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Booking successfully cancelled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id, 
            Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        boolean owns = bookingService.findForUser(user.getId()).stream().anyMatch(b -> b.getId().equals(id));
        if (!owns) return ResponseEntity.notFound().build();
        bookingService.cancelAndCompensate(id, UUID.randomUUID().toString());
        return ResponseEntity.noContent().build();
    }

    private Long pickRoom(LocalDate start, LocalDate end) {
        List<RoomDto> rooms = hotelClient.recommend(start, end);
        if (rooms.isEmpty()) {
            throw new IllegalStateException("No rooms available");
        }
        return rooms.get(0).getId();
    }
}


