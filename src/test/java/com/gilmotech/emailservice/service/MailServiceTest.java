package com.gilmotech.emailservice.service;

import com.gilmotech.emailservice.dto.MailRequestDto;
import com.gilmotech.emailservice.model.AppCode;
import com.gilmotech.emailservice.model.MailConfiguration;
import com.gilmotech.emailservice.model.MailType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ConfigurationService configService;

    @Mock
    private TemplateService templateService;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private MailService mailService;

    private MailRequestDto validRequest;
    private MailConfiguration testConfig;

    @BeforeEach
    void setUp() {
        validRequest = new MailRequestDto();
        validRequest.setAppCode("ASSURANTIS");
        validRequest.setMailType("CONTACT_FORM");
        validRequest.setName("Test User");
        validRequest.setEmail("test@example.com");
        validRequest.setPhone("+32 123 45 67 89");
        validRequest.setMessage("Test message");
        validRequest.setWebsite(""); // Honeypot vide

        testConfig = new MailConfiguration();
        testConfig.setAppCode(AppCode.ASSURANTIS);
        testConfig.setMailType(MailType.CONTACT_FORM);
        testConfig.setFromAddress("contact@assurantis.be");
        testConfig.setFromName("Assurantis");
        testConfig.setToAddresses(Arrays.asList("contact@assurantis.be"));
        testConfig.setSubject("Test Subject");
        testConfig.setTemplatePath("email/assurantis/contact");
    }

    @Test
    void sendMail_Success() {
        // Given
        when(configService.getConfiguration(any(), any())).thenReturn(testConfig);
        when(templateService.generateHtmlContent(any(), any())).thenReturn("<html>Test</html>");
        when(templateService.generateTextContent(any())).thenReturn("Test");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        assertDoesNotThrow(() -> mailService.sendMail(validRequest));

        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendMail_HoneypotFilled_ThrowsException() {
        // Given
        validRequest.setWebsite("http://spam.com");

        // When & Then
        assertThrows(Exception.class, () -> mailService.sendMail(validRequest));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}