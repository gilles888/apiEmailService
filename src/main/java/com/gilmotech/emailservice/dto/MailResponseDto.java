package com.gilmotech.emailservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailResponseDto {
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
    private String errorCode;

    public static MailResponseDto success(String message) {
        return new MailResponseDto(true, message, LocalDateTime.now(), null);
    }

    public static MailResponseDto error(String message, String errorCode) {
        return new MailResponseDto(false, message, LocalDateTime.now(), errorCode);
    }
}