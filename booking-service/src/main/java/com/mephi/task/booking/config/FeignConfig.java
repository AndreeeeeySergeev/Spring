package com.mephi.task.booking.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import feign.RequestInterceptor;

@Configuration
public class FeignConfig {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() instanceof String token) {
                template.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
        };
    }

    @Bean
    public RequestInterceptor dateFormattingInterceptor() {
        return template -> {
            // Исправляем формат дат в query параметрах
            Map<String, Collection<String>> queries = template.queries();
            if (queries != null && !queries.isEmpty()) {
                List<String> keysToFix = new ArrayList<>();
                for (Map.Entry<String, Collection<String>> entry : queries.entrySet()) {
                    String key = entry.getKey();
                    if (key.equals("start") || key.equals("end")) {
                        keysToFix.add(key);
                    }
                }
                
                // Пересоздаём параметры с правильным форматом
                for (String key : keysToFix) {
                    Collection<String> values = queries.get(key);
                    if (values != null) {
                        // Удаляем старые значения
                        template.query(key, (String[]) null);
                        // Добавляем правильно отформатированные
                        for (String value : values) {
                            if (value != null) {
                                String formatted = formatDate(value);
                                template.query(key, formatted);
                            }
                        }
                    }
                }
            }
        };
    }

    private String formatDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            // Пробуем распарсить в ISO формате
            LocalDate date = LocalDate.parse(value, ISO_DATE);
            return ISO_DATE.format(date);
        } catch (DateTimeParseException e1) {
            try {
                // Пробуем стандартный парсер
                LocalDate date = LocalDate.parse(value);
                return ISO_DATE.format(date);
            } catch (DateTimeParseException e2) {
                try {
                    // Пробуем форматы dd.MM.yy и dd.MM.yyyy
                    if (value.matches("\\d{2}\\.\\d{2}\\.\\d{2}")) {
                        // Формат dd.MM.yy (например, 28.10.25)
                        String[] parts = value.split("\\.");
                        int day = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int year = 2000 + Integer.parseInt(parts[2]);
                        LocalDate date = LocalDate.of(year, month, day);
                        return ISO_DATE.format(date);
                    } else if (value.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                        // Формат dd.MM.yyyy (например, 28.10.2025)
                        String[] parts = value.split("\\.");
                        int day = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int year = Integer.parseInt(parts[2]);
                        LocalDate date = LocalDate.of(year, month, day);
                        return ISO_DATE.format(date);
                    }
                } catch (Exception e3) {
                    // Если ничего не сработало
                }
                return value;
            }
        }
    }
}


