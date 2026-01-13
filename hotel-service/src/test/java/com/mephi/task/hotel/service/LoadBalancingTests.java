package com.mephi.task.hotel.service;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.mephi.task.hotel.domain.Hotel;
import com.mephi.task.hotel.domain.Room;
import com.mephi.task.hotel.repo.HotelRepository;
import com.mephi.task.hotel.repo.RoomHoldRepository;
import com.mephi.task.hotel.repo.RoomRepository;

@SpringBootTest
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class LoadBalancingTests {

    @Autowired
    AvailabilityService availabilityService;
    @Autowired
    HotelRepository hotelRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    RoomHoldRepository roomHoldRepository;

    private Hotel hotel;

    @BeforeEach
    @Transactional
    void setUp() {
        // Сначала удаляем все связанные записи из room_holds
        roomHoldRepository.deleteAll();
        // Затем удаляем комнаты
        roomRepository.deleteAll();
        // И в конце отели
        hotelRepository.deleteAll();

        hotel = new Hotel();
        hotel.setName("Balancing Hotel");
        hotel.setAddress("Test Address");
        hotel = hotelRepository.save(hotel);
    }

    @Test
    @DisplayName("Алгоритм равномерной загрузки: сортировка по возрастанию times_booked")
    void load_balancing_sorts_by_times_booked_ascending() {
        // Создаем комнаты с разным times_booked
        createRoom("101", 5L);  // Самый загруженный
        createRoom("102", 1L);  // Менее загруженный
        createRoom("103", 3L);  // Средний
        createRoom("104", 0L);  // Самый свободный

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        // Вызываем метод автоподбора
        var recommended = availabilityService.listRecommendedRooms(start, end);

        // Проверяем что комнаты отсортированы по times_booked ASC
        assertThat(recommended).hasSize(4);
        
        // Первой должна быть комната с times_booked = 0
        assertThat(recommended.get(0).getNumber()).isEqualTo("104");
        assertThat(recommended.get(0).getTimesBooked()).isEqualTo(0L);
        
        // Второй - с times_booked = 1
        assertThat(recommended.get(1).getNumber()).isEqualTo("102");
        assertThat(recommended.get(1).getTimesBooked()).isEqualTo(1L);
        
        // Третий - с times_booked = 3
        assertThat(recommended.get(2).getNumber()).isEqualTo("103");
        assertThat(recommended.get(2).getTimesBooked()).isEqualTo(3L);
        
        // Последний - с times_booked = 5
        assertThat(recommended.get(3).getNumber()).isEqualTo("101");
        assertThat(recommended.get(3).getTimesBooked()).isEqualTo(5L);
    }

    @Test
    @DisplayName("При равенстве times_booked - сортировка по ID")
    void load_balancing_when_times_booked_equal_sorts_by_id() {
        // Создаем комнаты с одинаковым times_booked
        createRoom("105", 2L);  // ID будет больше
        createRoom("106", 2L);  // ID будет больше чем room1
        createRoom("107", 2L);  // ID будет больше чем room2
        createRoom("108", 3L);  // Больше times_booked
        createRoom("109", 1L);  // Меньше times_booked

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        var recommended = availabilityService.listRecommendedRooms(start, end);

        // Комната с times_booked = 1 должна быть первой
        assertThat(recommended.get(0).getTimesBooked()).isEqualTo(1L);
        
        // Далее три комнаты с times_booked = 2, отсортированные по ID
        assertThat(recommended.get(1).getTimesBooked()).isEqualTo(2L);
        assertThat(recommended.get(2).getTimesBooked()).isEqualTo(2L);
        assertThat(recommended.get(3).getTimesBooked()).isEqualTo(2L);
        
        // Проверяем что ID идут по возрастанию
        assertThat(recommended.get(1).getId())
                .isLessThan(recommended.get(2).getId());
        assertThat(recommended.get(2).getId())
                .isLessThan(recommended.get(3).getId());
        
        // Последняя комната с times_booked = 3
        assertThat(recommended.get(4).getTimesBooked()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Автоподбор выбирает самую свободную комнату (минимальный times_booked)")
    void auto_selection_chooses_least_booked_room() {
        // Создаем 5 комнат с разной загрузкой
        createRoom("999", 100L);
        createRoom("888", 50L);
        createRoom("777", 10L);
        createRoom("666", 2L);
        createRoom("555", 0L);

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        var recommended = availabilityService.listRecommendedRooms(start, end);

        // Первой в списке должна быть самая свободная комната (times_booked = 0)
        Room firstRoom = recommended.get(0);
        assertThat(firstRoom.getNumber()).isEqualTo("555");
        assertThat(firstRoom.getTimesBooked()).isEqualTo(0L);
        
        // Проверяем полную сортировку
        assertThat(recommended.get(0).getTimesBooked()).isEqualTo(0L);   // free
        assertThat(recommended.get(1).getTimesBooked()).isEqualTo(2L);   // light
        assertThat(recommended.get(2).getTimesBooked()).isEqualTo(10L);  // average
        assertThat(recommended.get(3).getTimesBooked()).isEqualTo(50L);  // frequentlyBooked
        assertThat(recommended.get(4).getTimesBooked()).isEqualTo(100L); // overBooked
    }

    private Room createRoom(String number, long timesBooked) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(number);
        room.setAvailable(true);
        room.setTimesBooked(timesBooked);
        return roomRepository.save(room);
    }
}
