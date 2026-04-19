package com.libreuml.backend.application.emailverification.port.out;

public interface EmailSenderPort {
    void sendVerificationEmail(String to, String verificationUrl);
    void sendPasswordResetEmail(String to, String resetUrl);
    void sendOAuthAccountEmail(String to);
}
