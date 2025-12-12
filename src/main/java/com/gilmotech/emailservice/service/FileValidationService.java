package com.gilmotech.emailservice.service;

import com.gilmotech.emailservice.dto.FileAttachmentDto;
import com.gilmotech.emailservice.exception.MailSendingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class FileValidationService {

    // Types MIME autorisés
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
    );

    // Taille maximale par fichier : 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Taille maximale totale : 20MB
    private static final long MAX_TOTAL_SIZE = 20 * 1024 * 1024;

    /**
     * Valide tous les fichiers joints
     */
    public void validateAttachments(List<FileAttachmentDto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        log.info("Validation de {} fichier(s) joint(s)", attachments.size());

        long totalSize = 0;

        for (FileAttachmentDto attachment : attachments) {
            // Validation du type MIME
            if (!ALLOWED_MIME_TYPES.contains(attachment.getMimeType())) {
                throw new MailSendingException(
                        "INVALID_FILE_TYPE",
                        "Type de fichier non autorisé: " + attachment.getMimeType()
                );
            }

            // Validation du contenu Base64
            byte[] decodedContent;
            try {
                decodedContent = Base64.getDecoder().decode(attachment.getContent());
            } catch (IllegalArgumentException e) {
                throw new MailSendingException(
                        "INVALID_FILE_CONTENT",
                        "Le contenu du fichier n'est pas en Base64 valide: " + attachment.getFilename()
                );
            }

            // Validation de la taille individuelle
            long fileSize = decodedContent.length;
            if (fileSize > MAX_FILE_SIZE) {
                throw new MailSendingException(
                        "FILE_TOO_LARGE",
                        String.format("Le fichier '%s' dépasse la taille maximale de 5MB (taille: %.2f MB)",
                                attachment.getFilename(), fileSize / (1024.0 * 1024.0))
                );
            }

            totalSize += fileSize;

            log.debug("Fichier validé: {} - Type: {} - Taille: {} bytes",
                    attachment.getFilename(), attachment.getMimeType(), fileSize);
        }

        // Validation de la taille totale
        if (totalSize > MAX_TOTAL_SIZE) {
            throw new MailSendingException(
                    "TOTAL_SIZE_TOO_LARGE",
                    String.format("La taille totale des fichiers dépasse 20MB (taille: %.2f MB)",
                            totalSize / (1024.0 * 1024.0))
            );
        }

        log.info("Tous les fichiers sont valides. Taille totale: {} bytes", totalSize);
    }

    /**
     * Obtient l'extension du fichier à partir du nom
     */
    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Vérifie si le type MIME correspond à une image
     */
    public boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }
}