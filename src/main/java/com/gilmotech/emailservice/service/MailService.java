
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
        String adminHtmlContent = templateService.generateHtmlContent(
                config.getTemplatePath(),
                variables
        );
        String adminTextContent = templateService.generateTextContent(variables);

        // 5. Envoi de l'email
        try {
            sendEmailToAdmin(config, adminHtmlContent, adminTextContent, request.getEmail());
            log.info("Email admin envoyé avec succès pour {} / {}", appCode, mailType);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Erreur lors de l'envoi de l'email admin", e);
            throw new MailSendingException("ADMIN_SEND_FAILED", "Impossible d'envoyer l'email à l'admin", e);
        }

        // 6. Envoi de l'email de confirmation au client (si template défini)
        if (config.getTemplatePathConfirmation() != null && !config.getTemplatePathConfirmation().isEmpty()) {
            try {
                String confirmHtmlContent = templateService.generateHtmlContent(
                        config.getTemplatePathConfirmation(),
                        variables
                );
                String confirmTextContent = templateService.generateTextContent(variables);

                sendEmailToClient(config, confirmHtmlContent, confirmTextContent, request.getEmail());
                log.info("Email de confirmation envoyé avec succès à {} pour {} / {}",
                        request.getEmail(), appCode, mailType);
            } catch (MessagingException | UnsupportedEncodingException e) {
                log.error("Erreur lors de l'envoi de l'email de confirmation", e);
                // On ne throw pas d'exception ici pour ne pas bloquer si l'email admin est passé
                log.warn("L'email de confirmation n'a pas pu être envoyé, mais l'email admin a été envoyé");
            }
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


    private void sendEmailToAdmin(
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
        log.info("Envoi email ADMIN — expéditeur: {} <{}>, destinataires: {}, replyTo: {}",
                config.getFromName(), config.getFromAddress(),
                Arrays.toString(config.getToAddresses().toArray(new String[0])), userEmail);

        // Envoi
        mailSender.send(message);
    }


    private void sendEmailToClient(
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
        helper.setTo(userEmail);

        // Reply-To : l'adresse de l'entreprise
        helper.setReplyTo(config.getReplyTo() != null ? config.getReplyTo() : config.getFromAddress());


        // Sujet de confirmation
        String confirmSubject = config.getSubject().replace("Nouveau message", "Confirmation de votre message")
                .replace("Nouvelle demande", "Confirmation de votre demande");
        helper.setSubject(confirmSubject);

        // Contenu (HTML + texte en fallback)
        helper.setText(textContent, htmlContent);

        // Logs détaillés avant envoi
        log.info("Envoi email CONFIRMATION — expéditeur: {} <{}>, destinataire: {}, replyTo: {}",
                config.getFromName(), config.getFromAddress(), userEmail, config.getReplyTo());

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
