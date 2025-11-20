
package com.gilmotech.emailservice.service;

import com.gilmotech.emailservice.dto.MailRequestDto;
import com.gilmotech.emailservice.exception.MailSendingException;
import com.gilmotech.emailservice.model.AppCode;
import com.gilmotech.emailservice.model.MailConfiguration;
import com.gilmotech.emailservice.model.MailType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final ConfigurationService configService;
    private final TemplateService templateService;

    /**
     * Envoie un email basé sur la requête
     */
    public void sendMail(MailRequestDto request) {
        // 1. Vérification anti-bot (honeypot)
        if (request.getWebsite() != null && !request.getWebsite().isEmpty()) {
            log.warn("Tentative de spam détectée (honeypot rempli)");
            throw new MailSendingException("SPAM_DETECTED", "Requête invalide");
        }

        // 2. Récupération de la configuration
        AppCode appCode = AppCode.fromString(request.getAppCode());
        MailType mailType = MailType.fromString(request.getMailType());
        MailConfiguration config = configService.getConfiguration(appCode, mailType);

        // 3. Préparation des variables pour le template
        Map<String, Object> variables = prepareTemplateVariables(request);

        // 4. Génération du contenu
        String htmlContent = templateService.generateHtmlContent(
                config.getTemplatePath(),
                variables
        );
        String textContent = templateService.generateTextContent(variables);

        // 5. Envoi de l'email
        try {
            sendEmail(config, htmlContent, textContent, request.getEmail());
            log.info("Email envoyé avec succès pour {} / {}", appCode, mailType);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Erreur lors de l'envoi de l'email", e);
            throw new MailSendingException("SEND_FAILED", "Impossible d'envoyer l'email", e);
        }
    }

    private Map<String, Object> prepareTemplateVariables(MailRequestDto request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", sanitize(request.getName()));
        variables.put("email", request.getEmail());
        variables.put("phone", sanitize(request.getPhone()));
        variables.put("message", sanitize(request.getMessage()));
        variables.put("subject", sanitize(request.getSubject()));
        variables.put("company", sanitize(request.getCompany()));

        if (request.getAdditionalData() != null) {
            request.getAdditionalData().forEach((key, value) -> {
                if (value instanceof String) {
                    variables.put(key, sanitize((String) value));
                } else {
                    variables.put(key, value);
                }
            });
        }

        return variables;
    }

    private void sendEmail(
            MailConfiguration config,
            String htmlContent,
            String textContent,
            String userEmail
    ) throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Expéditeur
        helper.setFrom(
                new InternetAddress(config.getFromAddress(), config.getFromName())
        );

        // Destinataires
        helper.setTo(config.getToAddresses().toArray(new String[0]));

        if (config.getCcAddresses() != null && !config.getCcAddresses().isEmpty()) {
            helper.setCc(config.getCcAddresses().toArray(new String[0]));
        }

        if (config.getBccAddresses() != null && !config.getBccAddresses().isEmpty()) {
            helper.setBcc(config.getBccAddresses().toArray(new String[0]));
        }

        // Reply-To : l'utilisateur qui a rempli le formulaire
        helper.setReplyTo(userEmail);

        // Sujet
        helper.setSubject(config.getSubject());

        // Contenu (HTML + texte en fallback)
        helper.setText(textContent, htmlContent);

        // Logs détaillés avant envoi
        String fromAddr = config.getFromAddress();
        String fromName = config.getFromName() != null ? config.getFromName() : "";
        String[] to = config.getToAddresses().toArray(new String[0]);
        String[] cc = config.getCcAddresses() != null ? config.getCcAddresses().toArray(new String[0]) : new String[0];
        String[] bcc = config.getBccAddresses() != null ? config.getBccAddresses().toArray(new String[0]) : new String[0];

        log.info("Envoi email — expéditeur: {} <{}>, destinataires: {}, cc: {}, bcc: {}, replyTo: {}",
                fromName, fromAddr, Arrays.toString(to), Arrays.toString(cc), Arrays.toString(bcc), userEmail);

        // Envoi
        mailSender.send(message);
    }

    /**
     * Sanitize pour éviter l'injection de HTML/scripts
     */
    private String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;");
    }
}
