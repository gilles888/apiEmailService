package com.gilmotech.emailservice.integration;

import com.gilmotech.emailservice.dto.MailRequestDto;
import com.gilmotech.emailservice.service.MailService;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MailIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"))
            .withPerMethodLifecycle(false);

    @Autowired
    private MailService mailService;

    @Test
    void sendMail_Integration_Success() throws Exception {
        // Given
        MailRequestDto request = new MailRequestDto();
        request.setAppCode("ASSURANTIS");
        request.setMailType("CONTACT_FORM");
        request.setName("Integration Test");
        request.setEmail("integration@test.com");
        request.setPhone("+32 123 45 67 89");
        request.setMessage("Integration test message");
        request.setWebsite("");

        // When
        mailService.sendMail(request);

        // Then
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertEquals(1, receivedMessages.length);

        MimeMessage received = receivedMessages[0];
        assertTrue(received.getSubject().contains("contact"));
        assertNotNull(received.getContent());
    }
}