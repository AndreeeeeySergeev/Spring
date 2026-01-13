package com.mephi.task.booking.client.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AvailabilityRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String requestId;
    private String bookingId;
}


