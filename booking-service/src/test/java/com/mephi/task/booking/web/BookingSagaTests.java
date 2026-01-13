package com.mephi.task.booking.web;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingSagaTests {

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
    void setup() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();
        User u = new User();
        u.setUsername("u1");
        u.setPassword(new BCryptPasswordEncoder().encode("p1"));
        u.setRole("USER");
        userRepository.save(u);
        userToken = jwtService.generateToken(u.getUsername(), u.getRole(), 3600);
    }

    @Test
    @DisplayName("Saga: конфликт при подтверждении вызывает компенсацию и возвращает 409")
    void confirm_conflict_triggers_compensation_and_409() throws Exception {
        Mockito.doThrow(new RuntimeException("hotel conflict"))
                .when(hotelClient).confirmAvailability(ArgumentMatchers.eq(1L), ArgumentMatchers.any());

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
                .andExpect(status().isConflict());

        Booking b = bookingRepository.findAll().get(0);
        assertThat(b.getStatus().name()).isEqualTo("CANCELLED");
        Mockito.verify(hotelClient).release(ArgumentMatchers.eq(1L), ArgumentMatchers.eq(rid));
    }

    @Test
    @DisplayName("Saga: удаление бронирования вызывает release и переводит в статус CANCELLED")
    void delete_booking_invokes_release_and_sets_cancelled() throws Exception {
        Mockito.doNothing().when(hotelClient).confirmAvailability(ArgumentMatchers.eq(1L), ArgumentMatchers.any());
        String rid = java.util.UUID.randomUUID().toString();
        var body = new java.util.HashMap<String, Object>();
        body.put("autoSelect", false);
        body.put("roomId", 1);
        body.put("startDate", LocalDate.now().plusDays(1).toString());
        body.put("endDate", LocalDate.now().plusDays(2).toString());
        body.put("requestId", rid);

        String resp = mockMvc.perform(post("/api/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(resp);
        Long bookingId = json.get("id").asLong();

        mockMvc.perform(delete("/api/booking/" + bookingId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        Booking b = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(b.getStatus().name()).isEqualTo("CANCELLED");
        Mockito.verify(hotelClient, Mockito.atLeastOnce()).release(ArgumentMatchers.eq(1L), ArgumentMatchers.any());
    }
}





