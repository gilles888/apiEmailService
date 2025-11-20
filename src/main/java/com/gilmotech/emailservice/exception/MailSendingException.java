package com.gilmotech.emailservice.exception;

import lombok.Getter;

@Getter
public class MailSendingException extends RuntimeException {
    private final String errorCode;

    public MailSendingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MailSendingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}