package com.mephi.task.hotel.web.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorDto {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String traceId;
}





