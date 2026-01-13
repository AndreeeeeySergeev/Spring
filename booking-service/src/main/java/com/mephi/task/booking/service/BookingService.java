package com.mephi.task.booking.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mephi.task.booking.client.HotelClient;
import com.mephi.task.booking.client.dto.AvailabilityRequest;
import com.mephi.task.booking.domain.Booking;
import com.mephi.task.booking.domain.BookingStatus;
import com.mephi.task.booking.domain.User;
import com.mephi.task.booking.repo.BookingRepository;
import com.mephi.task.booking.repo.UserRepository;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final HotelClient hotelClient;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BookingService.class);

    @Transactional
    public Booking createPending(Long userId, Long roomId, LocalDate start, LocalDate end, String requestId) {
        log.info("createPending requestId={}, userId={}, roomId={}, start={}, end={}", requestId, userId, roomId, start, end);
        Optional<Booking> existing = bookingRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            log.info("createPending idempotent hit requestId={}, bookingId={}", requestId, existing.get().getId());
            return existing.get();
        }
        User user = userRepository.findById(userId).orElseThrow();
        Booking b = new Booking();
        b.setUser(user);
        b.setRoomId(roomId);
        b.setStartDate(start);
        b.setEndDate(end);
        b.setStatus(BookingStatus.PENDING);
        b.setCreatedAt(Instant.now());
        b.setRequestId(requestId);
        Booking saved = bookingRepository.save(b);
        log.info("created PENDING bookingId={} requestId={}", saved.getId(), requestId);
        return saved;
    }

    @Transactional
    @Retry(name = "hotel-confirm")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "hotel-confirm")
    public Booking confirm(Long bookingId) {
        Booking b = bookingRepository.findById(bookingId).orElseThrow();
        if (b.getStatus() == BookingStatus.CONFIRMED) {
            return b;
        }
        String correlationId = UUID.randomUUID().toString();
        log.info("confirm bookingId={} correlationId={} roomId={} start={} end={}", b.getId(), correlationId, b.getRoomId(), b.getStartDate(), b.getEndDate());
        hotelClient.confirmAvailability(b.getRoomId(), new AvailabilityRequest(b.getStartDate(), b.getEndDate(), correlationId, String.valueOf(b.getId())));
        b.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(b);
        log.info("confirmed bookingId={} correlationId={}", saved.getId(), correlationId);
        return saved;
    }

    @Transactional
    public Booking cancelAndCompensate(Long bookingId, String correlationId) {
        Booking b = bookingRepository.findById(bookingId).orElseThrow();
        if (b.getStatus() == BookingStatus.CANCELLED) {
            return b;
        }
        b.setStatus(BookingStatus.CANCELLED);
        log.warn("cancelAndCompensate bookingId={} correlationId={} roomId={}", b.getId(), correlationId, b.getRoomId());
        hotelClient.release(b.getRoomId(), correlationId);
        Booking saved = bookingRepository.save(b);
        log.info("cancelled bookingId={} correlationId={}", saved.getId(), correlationId);
        return saved;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Booking> findForUser(Long userId, org.springframework.data.domain.Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow();
        return bookingRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public java.util.List<Booking> findForUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return bookingRepository.findByUser(user, org.springframework.data.domain.Pageable.unpaged()).getContent();
    }
}


