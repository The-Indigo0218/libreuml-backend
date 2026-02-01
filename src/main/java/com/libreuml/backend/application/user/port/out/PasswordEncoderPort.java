package com.libreuml.backend.application.user.port.out;

public interface PasswordEncoderPort {


    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}