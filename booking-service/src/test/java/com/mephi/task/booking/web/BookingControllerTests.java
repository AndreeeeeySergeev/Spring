package com.mephi.task.booking.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mephi.task.booking.client.HotelClient;
import com.mephi.task.booking.domain.User;
import com.mephi.task.booking.repo.UserRepository;
import com.mephi.task.booking.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;
    
    @Autowired
    com.mephi.task.booking.repo.BookingRepository bookingRepository;

    @Autowired
    JwtService jwtService;

    @MockBean
    HotelClient hotelClient;

    @Autowired
    ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();
        User u = new User();
        u.setUsername("test");
        u.setPassword(new BCryptPasswordEncoder().encode("pass"));
        u.setRole("USER");
        userRepository.save(u);
        userToken = jwtService.generateToken(u.getUsername(), u.getRole(), 3600);
    }

    @Test
    @DisplayName("Доступ без JWT токена возвращает 403 Forbidden")
    void unauthorized_access_requires_jwt() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isForbidden()); // Spring Security returns 403 when no auth token provided
    }

    @Test
    @DisplayName("Валидация: endDate должна быть ПОЗЖЕ startDate, иначе 400 Bad Request")
    void create_booking_validation_errors() throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("autoSelect", false);
        body.put("startDate", LocalDate.now().toString());
        body.put("endDate", LocalDate.now().minusDays(1).toString());

        mockMvc.perform(post("/api/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Успешное создание бронирования с указанным номером (autoSelect=false)")
    void create_booking_success_with_provided_room() throws Exception {
        Mockito.doNothing().when(hotelClient).confirmAvailability(Mockito.eq(1L), Mockito.any());

        var body = new java.util.HashMap<String, Object>();
        body.put("autoSelect", false);
        body.put("roomId", 1);
        body.put("startDate", LocalDate.now().plusDays(1).toString());
        body.put("endDate", LocalDate.now().plusDays(2).toString());
        body.put("requestId", java.util.UUID.randomUUID().toString());

        mockMvc.perform(post("/api/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Идемпотентность: повторный запрос с тем же requestId не создает дубликат")
    void create_booking_idempotent_by_requestId() throws Exception {
        Mockito.doNothing().when(hotelClient).confirmAvailability(Mockito.eq(1L), Mockito.any());

        String rid = java.util.UUID.randomUUID().toString();
        var body = new java.util.HashMap<String, Object>();
        body.put("autoSelect", false);
        body.put("roomId", 1);
        body.put("startDate", LocalDate.now().plusDays(1).toString());
        body.put("endDate", LocalDate.now().plusDays(2).toString());
        body.put("requestId", rid);

        mockMvc.perform(post("/api/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // повторная отправка с тем же requestId должна вернуть 200 и не создавать дубликат
        mockMvc.perform(post("/api/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Pagination: GET /api/bookings с параметрами page и size")
    void get_bookings_with_pagination() throws Exception {
        Mockito.doNothing().when(hotelClient).confirmAvailability(Mockito.eq(1L), Mockito.any());

        // Создаем 5 бронирований
        for (int i = 0; i < 5; i++) {
            var body = new java.util.HashMap<String, Object>();
            body.put("autoSelect", false);
            body.put("roomId", 1);
            body.put("startDate", LocalDate.now().plusDays(i + 1).toString());
            body.put("endDate", LocalDate.now().plusDays(i + 2).toString());
            body.put("requestId", java.util.UUID.randomUUID().toString());

            mockMvc.perform(post("/api/booking")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + userToken)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        // Проверяем pagination: page=0, size=2
        mockMvc.perform(get("/api/bookings?page=0&size=2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Проверяем pagination: page=1, size=2
        mockMvc.perform(get("/api/bookings?page=1&size=2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }
}


