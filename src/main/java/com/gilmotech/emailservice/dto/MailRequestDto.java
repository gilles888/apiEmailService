package com.gilmotech.emailservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class MailRequestDto {

    @NotBlank(message = "Le code application est obligatoire")
    @Pattern(regexp = "ASSURANTIS|GILMOTECH",
            message = "Code application invalide")
    private String appCode;

    @NotBlank(message = "Le type de mail est obligatoire")
    @Pattern(regexp = "CONTACT_FORM|QUOTE_REQUEST|NEWSLETTER_SUBSCRIPTION|APPOINTMENT_REQUEST",
            message = "Type de mail invalide")
    private String mailType;

    // Données du formulaire
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String name;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{8,20}$",
            message = "Format de téléphone invalide")
    private String phone;

    @NotBlank(message = "Le message est obligatoire")
    @Size(min = 10, max = 2000, message = "Le message doit contenir entre 10 et 2000 caractères")
    private String message;

    private String subject;

    private String company;

    // Champ honeypot (anti-bot) - doit rester vide
    private String website;

    // Données additionnelles spécifiques
    private Map<String, Object> additionalData;

    // Token reCAPTCHA (optionnel)
    private String recaptchaToken;
}