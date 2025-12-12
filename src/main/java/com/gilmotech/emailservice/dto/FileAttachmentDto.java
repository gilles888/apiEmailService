package com.gilmotech.emailservice.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FileAttachmentDto {

    @NotBlank(message = "Le nom du fichier est obligatoire")
    @Size(max = 255, message = "Le nom du fichier ne peut pas dépasser 255 caractères")
    private String filename;

    @NotBlank(message = "Le contenu du fichier est obligatoire")
    private String content; // Base64 encoded

    @NotBlank(message = "Le type MIME est obligatoire")
    @Pattern(
            regexp = "^(image/(jpeg|jpg|png|gif|webp)|application/pdf|application/(vnd\\.openxmlformats-officedocument\\.(wordprocessingml\\.document|spreadsheetml\\.sheet)))$",
            message = "Type de fichier non autorisé"
    )
    private String mimeType;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;
}
