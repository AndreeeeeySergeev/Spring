package com.mephi.task.hotel;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.mephi.task.hotel.domain.Hotel;
import com.mephi.task.hotel.domain.Room;
import com.mephi.task.hotel.repo.HotelRepository;
import com.mephi.task.hotel.repo.RoomRepository;
import com.mephi.task.hotel.service.AvailabilityService;

@SpringBootTest
class AvailabilityConcurrencyTests {

    @Autowired
    AvailabilityService availabilityService;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    RoomRepository roomRepository;

    private Room room;

    @BeforeEach
    @Transactional
    void setup() {
        roomRepository.deleteAll();
        hotelRepository.deleteAll();
        Hotel h = new Hotel();
        h.setName("h");
        h.setAddress("a");
        h = hotelRepository.save(h);
        Room r = new Room();
        r.setHotel(h);
        r.setNumber("101");
        r.setAvailable(true);
        r.setTimesBooked(0L);
        room = roomRepository.save(r);
    }

    @Test
    @DisplayName("Потокобезопасность: два конкурентных бронирования одного номера - только одно успешно")
    void only_one_parallel_confirmation_succeeds() throws ExecutionException, InterruptedException {
        LocalDate s = LocalDate.now().plusDays(1);
        LocalDate e = s.plusDays(2);
        String bookingId = "b-1";

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Boolean> c1 = () -> availabilityService.confirmAvailability(room.getId(), s, e, UUID.randomUUID().toString(), bookingId);
        Callable<Boolean> c2 = () -> availabilityService.confirmAvailability(room.getId(), s, e, UUID.randomUUID().toString(), bookingId);

        Future<Boolean> f1 = pool.submit(c1);
        Future<Boolean> f2 = pool.submit(c2);
        boolean r1 = f1.get();
        boolean r2 = f2.get();

        // exactly one should be true
        assertThat(r1 ^ r2).isTrue();

        // timesBooked incremented exactly once
        Room reloaded = roomRepository.findById(room.getId()).orElseThrow();
        assertThat(reloaded.getTimesBooked()).isEqualTo(1L);
    }
}





