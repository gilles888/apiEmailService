package com.gilmotech.emailservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final TemplateEngine templateEngine;

    /**
     * Génère le contenu HTML à partir d'un template et des données
     */
    public String generateHtmlContent(String templatePath, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String html = templateEngine.process(templatePath, context);
            log.debug("Template généré avec succès: {}", templatePath);
            return html;

        } catch (Exception e) {
            log.error("Erreur lors de la génération du template: {}", templatePath, e);
            throw new RuntimeException("Erreur de génération du template", e);
        }
    }

    /**
     * Génère une version texte simple à partir des données
     * (fallback si le client mail ne supporte pas HTML)
     */
    public String generateTextContent(Map<String, Object> variables) {
        StringBuilder text = new StringBuilder();
        text.append("Nouveau message de contact\n\n");

        variables.forEach((key, value) -> {
            if (value != null && !key.equals("additionalData")) {
                text.append(capitalize(key)).append(": ").append(value).append("\n");
            }
        });

        return text.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}