package com.gilmotech.emailservice.model;

public enum AppCode {
    ASSURANTIS,
    GILMOTECH;

    public static AppCode fromString(String code) {
        try {
            return AppCode.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Code application invalide: " + code);
        }
    }
}
