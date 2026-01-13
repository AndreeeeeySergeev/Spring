package com.mephi.task.booking;

import com.mephi.task.booking.client.HotelClient;
import com.mephi.task.booking.domain.Booking;
import com.mephi.task.booking.domain.User;
import com.mephi.task.booking.repo.UserRepository;
import com.mephi.task.booking.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BookingFlowTests {

    @Autowired
    BookingService bookingService;
    @Autowired
    UserRepository userRepository;
    @MockBean
    HotelClient hotelClient;

    @Test
    @Transactional
    @DisplayName("Успешный поток создания бронирования: PENDING → CONFIRMED")
    void successFlow() {
        User u = new User();
        u.setUsername("u");
        u.setPassword("p");
        u.setRole("USER");
        u = userRepository.save(u);

        LocalDate s = LocalDate.now().plusDays(1);
        LocalDate e = s.plusDays(2);

        Booking pending = bookingService.createPending(u.getId(), 1L, s, e, "req-1");
        Mockito.doNothing().when(hotelClient).confirmAvailability(Mockito.eq(1L), Mockito.any());
        Booking confirmed = bookingService.confirm(pending.getId());
        assertThat(confirmed.getId()).isNotNull();
    }
}


