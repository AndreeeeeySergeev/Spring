package com.mephi.task.hotel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import com.mephi.task.hotel.domain.Hotel;
import com.mephi.task.hotel.domain.Room;
import com.mephi.task.hotel.repo.HotelRepository;
import com.mephi.task.hotel.repo.RoomRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DemoDataLoader {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @PostConstruct
    @Transactional
    public void seed() {
        if (hotelRepository.count() > 0) {
            return;
        }

        Hotel h1 = new Hotel();
        h1.setName("Aurora");
        h1.setAddress("Nevsky 1, SPB");
        h1 = hotelRepository.save(h1);

        Hotel h2 = new Hotel();
        h2.setName("Vostok");
        h2.setAddress("Tverskaya 10, Moscow");
        h2 = hotelRepository.save(h2);

        createRoom(h1, "101");
        createRoom(h1, "102");
        createRoom(h2, "201");
        createRoom(h2, "202");
    }

    private void createRoom(Hotel hotel, String number) {
        Room r = new Room();
        r.setHotel(hotel);
        r.setNumber(number);
        r.setAvailable(true);
        r.setTimesBooked(0);
        roomRepository.save(r);
    }
}


