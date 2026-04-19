package com.libreuml.backend.domain.model;

public enum AuditEventType {
    USER_REGISTERED,
    USER_LOGIN,
    USER_LOGIN_FAILED,
    USER_LOGOUT,
    OAUTH_LOGIN,
    EMAIL_VERIFIED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,
    API_KEY_CREATED,
    API_KEY_REVOKED
}
