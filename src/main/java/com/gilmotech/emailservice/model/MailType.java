package com.gilmotech.emailservice.model;

public enum MailType {CONTACT_FORM,
    QUOTE_REQUEST,
    NEWSLETTER_SUBSCRIPTION,
    APPOINTMENT_REQUEST;

    public static MailType fromString(String type) {
        try {
            return MailType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type de mail invalide: " + type);
        }
    }
}
