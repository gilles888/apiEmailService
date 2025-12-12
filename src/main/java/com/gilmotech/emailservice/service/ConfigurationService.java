package com.gilmotech.emailservice.service;

import com.gilmotech.emailservice.model.AppCode;
import com.gilmotech.emailservice.model.MailConfiguration;
import com.gilmotech.emailservice.model.MailType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
@Slf4j
public class ConfigurationService {

    private final Map<String, MailConfiguration> configurations = new HashMap<>();

    @PostConstruct
    public void init() {
        // Configuration Assurantis - Contact
        MailConfiguration assurantisContact = new MailConfiguration();
        assurantisContact.setAppCode(AppCode.ASSURANTIS);
        assurantisContact.setMailType(MailType.CONTACT_FORM);
        assurantisContact.setFromAddress("contact@assurantis.be");
        assurantisContact.setFromName("Assurantis");
        assurantisContact.setToAddresses(List.of("contact@assurantis.be"));
        assurantisContact.setReplyTo("contact@assurantis.be");
        assurantisContact.setSubject("Nouveau message de contact - Assurantis");
        assurantisContact.setTemplatePath("email/assurantis/contact_admin");
        assurantisContact.setTemplatePathConfirmation("email/assurantis/contact_confirmation");
        assurantisContact.setActive(true);

        // Configuration Assurantis - Demande de Devis
        MailConfiguration assurantisQuote = new MailConfiguration();
        assurantisQuote.setAppCode(AppCode.ASSURANTIS);
        assurantisQuote.setMailType(MailType.QUOTE_REQUEST);
        assurantisQuote.setFromAddress("contact@assurantis.be");
        assurantisQuote.setFromName("Assurantis - Demandes de Devis");
        assurantisQuote.setToAddresses(List.of("contact@assurantis.be"));
        assurantisQuote.setReplyTo("contact@assurantis.be");
        assurantisQuote.setSubject("Nouvelle demande de devis - Assurantis");
        assurantisQuote.setTemplatePath("email/assurantis/quote_admin");
        assurantisQuote.setTemplatePathConfirmation("email/assurantis/quote_confirmation");
        assurantisQuote.setActive(true);

        // Configuration Assurantis - Déclaration de Sinistre
        MailConfiguration assurantisClaim = new MailConfiguration();
        assurantisClaim.setAppCode(AppCode.ASSURANTIS);
        assurantisClaim.setMailType(MailType.CLAIM_REQUEST);
        assurantisClaim.setFromAddress("contact@assurantis.be");
        assurantisClaim.setFromName("Assurantis - Déclarations de Sinistre");
        assurantisClaim.setToAddresses(List.of("contact@assurantis.be"));
        assurantisClaim.setReplyTo("contact@assurantis.be");
        assurantisClaim.setSubject("Nouvelle déclaration de sinistre - Assurantis");
        assurantisClaim.setTemplatePath("email/assurantis/claim_admin");
        assurantisClaim.setTemplatePathConfirmation("email/assurantis/claim_confirmation");
        assurantisClaim.setActive(true);

        // Configuration Gilmotech - Contact
        MailConfiguration gilmotechContact = new MailConfiguration();
        gilmotechContact.setAppCode(AppCode.GILMOTECH);
        gilmotechContact.setMailType(MailType.CONTACT_FORM);
        gilmotechContact.setFromAddress("contact@gilmotech.be");
        gilmotechContact.setFromName("Gilmotech");
        gilmotechContact.setToAddresses(List.of("contact@gilmotech.be"));
        gilmotechContact.setReplyTo("contact@gilmotech.be");
        gilmotechContact.setSubject("Nouveau message de contact - Gilmotech");
        gilmotechContact.setTemplatePath("email/gilmotech/contact");
        gilmotechContact.setActive(true);

        // Ajouter à la map
        configurations.put(getKey(AppCode.ASSURANTIS, MailType.CONTACT_FORM), assurantisContact);
        configurations.put(getKey(AppCode.ASSURANTIS, MailType.QUOTE_REQUEST), assurantisQuote);
        configurations.put(getKey(AppCode.ASSURANTIS, MailType.CLAIM_REQUEST), assurantisClaim);
        configurations.put(getKey(AppCode.GILMOTECH, MailType.CONTACT_FORM), gilmotechContact);

        log.info("Configurations chargées: {}", configurations.size());
    }

    public MailConfiguration getConfiguration(AppCode appCode, MailType mailType) {
        String key = getKey(appCode, mailType);
        MailConfiguration config = configurations.get(key);

        if (config == null || !config.isActive()) {
            throw new IllegalArgumentException(
                    String.format("Configuration non trouvée pour %s / %s", appCode, mailType)
            );
        }

        return config;
    }

    private String getKey(AppCode appCode, MailType mailType) {
        return appCode + "_" + mailType;
    }
}