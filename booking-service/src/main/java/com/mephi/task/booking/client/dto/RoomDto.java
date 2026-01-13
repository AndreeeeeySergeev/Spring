package com.mephi.task.booking.client.dto;

import lombok.Data;

@Data
public class RoomDto {
    private Long id;
    private Long hotelId;
    private String number;
    private boolean available;
    private long timesBooked;
}


