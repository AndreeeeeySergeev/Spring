package com.mephi.task.hotel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mephi.task.hotel.domain.Hotel;
import com.mephi.task.hotel.domain.Room;
import com.mephi.task.hotel.domain.RoomHold;
import com.mephi.task.hotel.repo.HotelRepository;
import com.mephi.task.hotel.repo.RoomHoldRepository;
import com.mephi.task.hotel.repo.RoomRepository;
import com.mephi.task.hotel.web.dto.RoomStatsDto;
import com.mephi.task.hotel.security.JwtService;
import io.jsonwebtoken.Jwts;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class RoomControllerStatsTests {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    RoomHoldRepository roomHoldRepository;
    @Autowired
    JwtService jwtService;

    private Room room;

    @BeforeEach
    void setUp() {
        roomHoldRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();

        Hotel h = new Hotel();
        h.setName("Test Hotel");
        h.setAddress("Addr");
        h = hotelRepository.save(h);

        Room r = new Room();
        r.setHotel(h);
        r.setNumber("101");
        r.setAvailable(true);
        r.setTimesBooked(2L);
        room = roomRepository.save(r);

        // active hold spanning today
        RoomHold hold = new RoomHold();
        hold.setRoom(room);
        hold.setStartDate(LocalDate.now().minusDays(1));
        hold.setEndDate(LocalDate.now().plusDays(1));
        hold.setRequestId("req-1");
        hold.setBookingId("b-1");
        roomHoldRepository.save(hold);

        // past hold, should not be counted
        RoomHold oldHold = new RoomHold();
        oldHold.setRoom(room);
        oldHold.setStartDate(LocalDate.now().minusDays(10));
        oldHold.setEndDate(LocalDate.now().minusDays(5));
        oldHold.setRequestId("req-2");
        oldHold.setBookingId("b-2");
        roomHoldRepository.save(oldHold);
    }

    @Test
    @DisplayName("Статистика номеров: возвращает агрегированные данные (timesBooked, activeHolds)")
    void stats_returns_aggregated_fields_and_active_holds() throws Exception {
        String token = Jwts.builder()
                .setSubject("tester")
                .claim("role", "USER")
                .signWith(jwtService.getKey())
                .compact();

        String json = mockMvc.perform(get("/api/rooms/stats")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var list = objectMapper.readValue(json, new TypeReference<java.util.List<RoomStatsDto>>() {});
        assertThat(list).hasSize(1);
        RoomStatsDto dto = list.get(0);
        assertThat(dto.getRoomId()).isEqualTo(room.getId());
        assertThat(dto.getRoomNumber()).isEqualTo("101");
        assertThat(dto.getHotelId()).isEqualTo(room.getHotel().getId());
        assertThat(dto.getHotelName()).isEqualTo("Test Hotel");
        assertThat(dto.getTimesBooked()).isEqualTo(2L);
        assertThat(dto.getActiveHolds()).isEqualTo(1L);
    }
}


