package com.mephi.task.hotel.web.dto;

import lombok.Data;

@Data
public class RoomStatsDto {
    private Long roomId;
    private String roomNumber;
    private Long hotelId;
    private String hotelName;
    private long timesBooked;
    private long activeHolds;
}





