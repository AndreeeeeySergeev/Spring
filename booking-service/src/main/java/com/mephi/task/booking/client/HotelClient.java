package com.mephi.task.booking.client;

import java.time.LocalDate;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.mephi.task.booking.client.dto.AvailabilityRequest;
import com.mephi.task.booking.client.dto.RoomDto;

@FeignClient(name = "hotel-service", url = "http://localhost:8082")
public interface HotelClient {

    @GetMapping("/api/rooms/internal/recommend")
    List<RoomDto> recommend(@RequestParam("start") @feign.Param(expander = com.mephi.task.booking.config.LocalDateParamExpander.class) LocalDate start,
                            @RequestParam("end") @feign.Param(expander = com.mephi.task.booking.config.LocalDateParamExpander.class) LocalDate end);

    @PostMapping("/api/rooms/{id}/confirm-availability")
    void confirmAvailability(@PathVariable("id") Long roomId, @RequestBody AvailabilityRequest request);

    @PostMapping("/internal/rooms/{id}/release")
    void release(@PathVariable("id") Long roomId, @RequestParam("requestId") String requestId);
}


