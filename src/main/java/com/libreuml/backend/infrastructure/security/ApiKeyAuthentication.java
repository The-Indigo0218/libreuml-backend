package com.libreuml.backend.infrastructure.security;

import com.libreuml.backend.domain.model.ApiKey;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Authentication token for partner API keys that are not yet linked to a user account
 * (i.e. {@code user_id IS NULL}).
 *
 * <p>For key holders with a linked user, the filter creates a standard
 * {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken}
 * carrying a {@link CustomUserDetails} principal, so that existing controllers that
 * {@code @AuthenticationPrincipal CustomUserDetails} cast work without modification.
 *
 * <p>This class is used only for unlinked partner keys consumed by machine-to-machine
 * or MCP agent endpoints that are explicitly aware of the non-user principal type.
 */
public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final ApiKey apiKey;

    public ApiKeyAuthentication(ApiKey apiKey, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return apiKey;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }
}
