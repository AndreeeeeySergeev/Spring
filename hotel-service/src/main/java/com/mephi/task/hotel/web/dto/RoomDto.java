package com.mephi.task.hotel.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomDto {
    private Long id;
    @NotNull
    private Long hotelId;
    @NotBlank
    private String number;
    private boolean available = true;
    private Long timesBooked;
}


