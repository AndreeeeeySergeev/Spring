package com.mephi.task.hotel;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.mephi.task.hotel.domain.Hotel;
import com.mephi.task.hotel.domain.Room;
import com.mephi.task.hotel.repo.HotelRepository;
import com.mephi.task.hotel.repo.RoomRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class HotelDataLoader implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        if (hotelRepository.count() > 0) return;

        Hotel h1 = new Hotel();
        h1.setName("Grand Plaza");
        h1.setAddress("1 Main St");
        h1 = hotelRepository.save(h1);

        Hotel h2 = new Hotel();
        h2.setName("City Inn");
        h2.setAddress("2 Center Ave");
        h2 = hotelRepository.save(h2);

        Room r1 = new Room();
        r1.setHotel(h1);
        r1.setNumber("101");
        r1.setAvailable(true);
        r1.setTimesBooked(0);

        Room r2 = new Room();
        r2.setHotel(h1);
        r2.setNumber("102");
        r2.setAvailable(true);
        r2.setTimesBooked(3);

        Room r3 = new Room();
        r3.setHotel(h2);
        r3.setNumber("201");
        r3.setAvailable(true);
        r3.setTimesBooked(1);

        roomRepository.saveAll(List.of(r1, r2, r3));
    }
}





