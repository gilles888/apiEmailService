package com.gilmotech.emailservice.controller;

import com.gilmotech.emailservice.dto.MailRequestDto;
import com.gilmotech.emailservice.dto.MailResponseDto;
import com.gilmotech.emailservice.exception.MailSendingException;
import com.gilmotech.emailservice.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Slf4j
public class MailController {

    private final MailService mailService;

    @PostMapping("/send")
    public ResponseEntity<MailResponseDto> sendMail(
            @Valid @RequestBody MailRequestDto request
    ) {
        try {
            log.info("Réception d'une demande d'envoi de mail: {} / {}",
                    request.getAppCode(), request.getMailType());

            mailService.sendMail(request);

            return ResponseEntity.ok(
                    MailResponseDto.success("Email envoyé avec succès")
            );

        } catch (MailSendingException e) {
            log.error("Erreur lors de l'envoi: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(MailResponseDto.error(e.getMessage(), e.getErrorCode()));

        } catch (IllegalArgumentException e) {
            log.error("Argument invalide: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(MailResponseDto.error(e.getMessage(), "INVALID_ARGUMENT"));

        } catch (Exception e) {
            log.error("Erreur interne", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MailResponseDto.error(
                            "Une erreur s'est produite lors de l'envoi",
                            "INTERNAL_ERROR"
                    ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Email service is running");
    }
}