package com.libreuml.backend.application.emailverification.port.out;

public interface EmailSenderPort {
    void sendVerificationEmail(String to, String verificationUrl);
}
