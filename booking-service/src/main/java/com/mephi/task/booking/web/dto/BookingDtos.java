package com.mephi.task.booking.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class BookingDtos {
    @Data
    public static class CreateBookingRequest {
        private Long roomId;
        @NotNull
        private LocalDate startDate;
        @NotNull
        private LocalDate endDate;
        private boolean autoSelect;
        // client-provided for idempotency of create
        @NotNull
        private String requestId;
    }
}


