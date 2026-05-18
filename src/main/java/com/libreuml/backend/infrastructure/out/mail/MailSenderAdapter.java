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
        sendEmail(to, "Verify your LibreUML email address",
                "templates/email/verification_en.html",
                "templates/email/verification_en.txt",
                new String[]{"{{verificationUrl}}", "{{email}}"},
                new String[]{verificationUrl, to});
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetUrl) {
        sendEmail(to, "Reset your LibreUML password",
                "templates/email/password_reset_en.html",
                "templates/email/password_reset_en.txt",
                new String[]{"{{resetUrl}}", "{{email}}"},
                new String[]{resetUrl, to});
    }

    @Override
    public void sendOAuthAccountEmail(String to) {
        sendEmail(to, "Password reset requested — LibreUML",
                "templates/email/oauth_account_en.html",
                "templates/email/oauth_account_en.txt",
                new String[]{"{{email}}"},
                new String[]{to});
    }

    private void sendEmail(String to, String subject,
                           String htmlTemplate, String textTemplate,
                           String[] placeholders, String[] values) {
        try {
            String html = loadTemplate(htmlTemplate);
            String text = loadTemplate(textTemplate);
            for (int i = 0; i < placeholders.length; i++) {
                html = html.replace(placeholders[i], values[i]);
                text = text.replace(placeholders[i], values[i]);
            }

            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }

    private String loadTemplate(String path) throws IOException {
        var resource = new ClassPathResource(path);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
