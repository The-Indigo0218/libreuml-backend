package com.libreuml.backend.infrastructure.out.mail;

import com.libreuml.backend.application.emailverification.port.out.EmailSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class MailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendVerificationEmail(String to, String verificationUrl) {
        try {
            String html = loadTemplate("templates/email/verification_en.html")
                    .replace("{{verificationUrl}}", verificationUrl)
                    .replace("{{email}}", to);
            String text = loadTemplate("templates/email/verification_en.txt")
                    .replace("{{verificationUrl}}", verificationUrl)
                    .replace("{{email}}", to);

            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Verify your LibreUML email address");
            helper.setText(text, html);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email to " + to, e);
        }
    }

    private String loadTemplate(String path) throws IOException {
        var resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
