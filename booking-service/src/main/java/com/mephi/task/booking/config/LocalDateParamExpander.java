package com.mephi.task.booking.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import feign.Param;

public class LocalDateParamExpander implements Param.Expander {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;

    @Override
    public String expand(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return ISO_DATE.format(date);
        }
        if (value instanceof String str) {
            // Если это строка, пытаемся распарсить и переформатировать в ISO
            try {
                // Сначала пробуем ISO формат
                LocalDate date = LocalDate.parse(str, ISO_DATE);
                return ISO_DATE.format(date);
            } catch (DateTimeParseException e1) {
                try {
                    // Пробуем стандартный парсер (ISO_LOCAL_DATE)
                    LocalDate date = LocalDate.parse(str);
                    return ISO_DATE.format(date);
                } catch (DateTimeParseException e2) {
                    try {
                        // Пробуем другие форматы: dd.MM.yy, dd.MM.yyyy
                        if (str.matches("\\d{2}\\.\\d{2}\\.\\d{2}")) {
                            // Формат dd.MM.yy (например, 28.10.25)
                            String[] parts = str.split("\\.");
                            int day = Integer.parseInt(parts[0]);
                            int month = Integer.parseInt(parts[1]);
                            int year = 2000 + Integer.parseInt(parts[2]); // Предполагаем 20xx
                            LocalDate date = LocalDate.of(year, month, day);
                            return ISO_DATE.format(date);
                        } else if (str.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                            // Формат dd.MM.yyyy (например, 28.10.2025)
                            String[] parts = str.split("\\.");
                            int day = Integer.parseInt(parts[0]);
                            int month = Integer.parseInt(parts[1]);
                            int year = Integer.parseInt(parts[2]);
                            LocalDate date = LocalDate.of(year, month, day);
                            return ISO_DATE.format(date);
                        }
                    } catch (Exception e3) {
                        // Если ничего не сработало, возвращаем как есть
                    }
                    return str;
                }
            }
        }
        // Для списков дат
        if (value instanceof Iterable<?> iterable) {
            List<String> formatted = new ArrayList<>();
            for (Object item : iterable) {
                if (item instanceof LocalDate date) {
                    formatted.add(ISO_DATE.format(date));
                } else if (item != null) {
                    String expanded = expand(item);
                    formatted.add(expanded != null ? expanded : item.toString());
                }
            }
            return String.join(",", formatted);
        }
        // Если это не LocalDate и не String, пытаемся преобразовать в строку и распарсить
        String strValue = value.toString();
        if (!strValue.equals(value.getClass().getName())) {
            return expand(strValue);
        }
        return strValue;
    }
}

