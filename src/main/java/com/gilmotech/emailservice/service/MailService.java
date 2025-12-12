package com.gilmotech.emailservice.service;

import com.gilmotech.emailservice.dto.FileAttachmentDto;
import com.gilmotech.emailservice.dto.MailRequestDto;
import com.gilmotech.emailservice.exception.MailSendingException;
import com.gilmotech.emailservice.model.AppCode;
import com.gilmotech.emailservice.model.MailConfiguration;
import com.gilmotech.emailservice.model.MailType;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final ConfigurationService configService;
    private final TemplateService templateService;
    private final FileValidationService fileValidationService;

    /**
     * Envoie un email basé sur la requête
     * @return le numéro de référence généré
     */
    public String sendMail(MailRequestDto request) {
        // 1. Vérification anti-bot (honeypot)
        if (request.getWebsite() != null && !request.getWebsite().isEmpty()) {
            log.warn("Tentative de spam détectée (honeypot rempli)");
            throw new MailSendingException("SPAM_DETECTED", "Requête invalide");
        }

        // 2. Validation des fichiers joints
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            fileValidationService.validateAttachments(request.getAttachments());
            log.info("Validation des {} pièce(s) jointe(s) réussie", request.getAttachments().size());
        }

        // 3. Récupération de la configuration
        AppCode appCode = AppCode.fromString(request.getAppCode());
        MailType mailType = MailType.fromString(request.getMailType());
        MailConfiguration config = configService.getConfiguration(appCode, mailType);

        // 4. Préparation des variables pour le template
        Map<String, Object> variables = prepareTemplateVariables(request);
        String reference = (String) variables.get("reference");

        // 5. Génération du contenu
        String adminHtmlContent = templateService.generateHtmlContent(
                config.getTemplatePath(),
                variables
        );
        String adminTextContent = templateService.generateTextContent(variables);

        // 6. Envoi de l'email à l'admin avec pièces jointes
        try {
            sendEmailToAdmin(
                    config,
                    adminHtmlContent,
                    adminTextContent,
                    request.getEmail(),
                    request.getAttachments()
            );
            log.info("Email admin envoyé avec succès pour {} / {}", appCode, mailType);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Erreur lors de l'envoi de l'email admin", e);
            throw new MailSendingException("ADMIN_SEND_FAILED", "Impossible d'envoyer l'email à l'admin", e);
        }

        // 7. Envoi de l'email de confirmation au client (si template défini)
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
                log.warn("L'email de confirmation n'a pas pu être envoyé, mais l'email admin a été envoyé");
            }
        }

        // Retourner la référence générée
        return reference;
    }

    private Map<String, Object> prepareTemplateVariables(MailRequestDto request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", sanitize(request.getName()));
        variables.put("email", request.getEmail());
        variables.put("phone", sanitize(request.getPhone()));
        variables.put("message", sanitize(request.getMessage()));
        variables.put("subject", sanitize(request.getSubject()));
        variables.put("company", sanitize(request.getCompany()));

        // Ajout des informations sur les pièces jointes
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            variables.put("hasAttachments", true);
            variables.put("attachmentCount", request.getAttachments().size());
            variables.put("attachments", request.getAttachments());
        } else {
            variables.put("hasAttachments", false);
            variables.put("attachmentCount", 0);
        }

        // Génération du numéro de référence (pour les sinistres et devis)
        String reference = generateReference(request.getMailType());
        variables.put("reference", reference);

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

    /**
     * Génère un numéro de référence unique selon le type de mail
     */
    private String generateReference(String mailType) {
        String prefix;
        switch (mailType.toUpperCase()) {
            case "CLAIM_REQUEST":
                prefix = "SIN";
                break;
            case "QUOTE_REQUEST":
                prefix = "DEV";
                break;
            case "CONTACT_FORM":
                prefix = "CNT";
                break;
            default:
                prefix = "REF";
        }

        // Format: PREFIX-YYYYMMDD-XXXX (ex: SIN-20241211-1234)
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", (int)(Math.random() * 10000));

        return String.format("%s-%s-%s", prefix, timestamp, randomPart);
    }

    private void sendEmailToAdmin(
            MailConfiguration config,
            String htmlContent,
            String textContent,
            String userEmail,
            List<FileAttachmentDto> attachments
    ) throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();

        // Activer le multipart pour supporter les pièces jointes
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Expéditeur
        helper.setFrom(new InternetAddress(config.getFromAddress(), config.getFromName()));

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
        String subject = config.getSubject();
        if (attachments != null && !attachments.isEmpty()) {
            subject += " (avec " + attachments.size() + " pièce(s) jointe(s))";
        }
        helper.setSubject(subject);

        // Contenu (HTML + texte en fallback)
        helper.setText(textContent, htmlContent);

        // Ajout des pièces jointes
        if (attachments != null && !attachments.isEmpty()) {
            for (FileAttachmentDto attachment : attachments) {
                try {
                    byte[] fileContent = Base64.getDecoder().decode(attachment.getContent());
                    DataSource dataSource = new ByteArrayDataSource(fileContent, attachment.getMimeType());
                    helper.addAttachment(attachment.getFilename(), dataSource);

                    log.debug("Pièce jointe ajoutée: {} ({} bytes)",
                            attachment.getFilename(), fileContent.length);
                } catch (Exception e) {
                    log.error("Erreur lors de l'ajout de la pièce jointe: {}", attachment.getFilename(), e);
                    throw new MailSendingException(
                            "ATTACHMENT_ERROR",
                            "Erreur lors de l'ajout de la pièce jointe: " + attachment.getFilename(),
                            e
                    );
                }
            }
        }

        // Logs détaillés avant envoi
        log.info("Envoi email ADMIN — expéditeur: {} <{}>, destinataires: {}, replyTo: {}, pièces jointes: {}",
                config.getFromName(), config.getFromAddress(),
                config.getToAddresses(), userEmail,
                attachments != null ? attachments.size() : 0);

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
        helper.setFrom(new InternetAddress(config.getFromAddress(), config.getFromName()));

        // Destinataires
        helper.setTo(userEmail);

        // Reply-To : l'adresse de l'entreprise
        helper.setReplyTo(config.getReplyTo() != null ? config.getReplyTo() : config.getFromAddress());

        // Sujet de confirmation
        String confirmSubject = config.getSubject()
                .replace("Nouveau message", "Confirmation de votre message")
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