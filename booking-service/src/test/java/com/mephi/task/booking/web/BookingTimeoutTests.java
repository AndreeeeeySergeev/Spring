package com.mephi.task.booking.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mephi.task.booking.client.HotelClient;
import com.mephi.task.booking.domain.Booking;
import com.mephi.task.booking.domain.User;
import com.mephi.task.booking.repo.BookingRepository;
import com.mephi.task.booking.repo.UserRepository;
import com.mephi.task.booking.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты проверяющие поведение системы при тайм-аутах и ошибках Hotel Service
 * 
 * Проверяемые сценарии:
 * - Ошибка при вызове Hotel Service (симулирует тайм-аут)
 * - Компенсация при ошибке
 * - Переход PENDING → CANCELLED
 * - Вызов release при компенсации
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingTimeoutTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    HotelClient hotelClient;

    private String userToken;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();
        
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        user.setRole("USER");
        userRepository.save(user);
        userToken = jwtService.generateToken(user.getUsername(), user.getRole(), 3600);
    }

    @Test
    @DisplayName("Тайм-аут при подтверждении доступности номера вызывает компенсацию")
    void timeout_triggers_compensation_and_cancellation() throws Exception {
        // Настраиваем HotelClient для генерации тайм-аута (симуляция через RuntimeException)
        doThrow(new RuntimeException("Hotel service timeout"))
                .when(hotelClient).confirmAvailability(eq(1L), any());

        var body = new java.util.HashMap<String, Object>();
        body.put("autoSelect", false);
        body.put("roomId", 1);
        body.put("startDate", LocalDate.now().plusDays(1).toString());
        body.put("endDate", LocalDate.now().plusDays(2).toString());
        body.put("requestId", "timeout-test-" + System.currentTimeMillis());

        // Отправляем запрос на создание бронирования
        mockMvc.perform(post("/api/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict()); // 409 при тайм-ауте

        // Проверяем, что бронирование создано и переведено в статус CANCELLED
        Booking booking = bookingRepository.findAll().stream()
                .findFirst()
                .orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(com.mephi.task.booking.domain.BookingStatus.CANCELLED);
        assertThat(booking.getRequestId()).isNotNull();

        // Проверяем, что была вызвана компенсация
        verify(hotelClient, org.mockito.Mockito.atLeastOnce())
                .release(eq(1L), any(String.class));
    }

    @Test
    @DisplayName("Идемпотентность при тайм-ауте: повторный запрос с тем же requestId возвращает то же бронирование")
    void timeout_idempotency_same_requestId_returns_same_booking() throws Exception {
        doThrow(new RuntimeException("Timeout"))
                .when(hotelClient).confirmAvailability(eq(1L), any());

        String requestId = "idempotency-test-" + System.currentTimeMillis();
        var body = new java.util.HashMap<String, Object>();
        body.put("autoSelect", false);
        body.put("roomId", 1);
        body.put("startDate", LocalDate.now().plusDays(1).toString());
        body.put("endDate", LocalDate.now().plusDays(2).toString());
        body.put("requestId", requestId);

        // Первый запрос
        mockMvc.perform(post("/api/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());

        Long firstBookingId = bookingRepository.findAll().stream()
                .findFirst()
                .map(Booking::getId)
                .orElseThrow();

        // Второй запрос с тем же requestId
        mockMvc.perform(post("/api/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());

        Long secondBookingId = bookingRepository.findAll().stream()
                .findFirst()
                .map(Booking::getId)
                .orElseThrow();

        // Проверяем, что вернулось то же бронирование (идемпотентность)
        assertThat(firstBookingId).isEqualTo(secondBookingId);
        
        // В базе должно быть только одно бронирование
        assertThat(bookingRepository.count()).isEqualTo(1);
    }
}

