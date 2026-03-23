package com.libreuml.backend.application.auth.port.service;

import com.libreuml.backend.application.auth.dto.OAuthProvider;
import com.libreuml.backend.application.auth.exception.OAuthException;
import com.libreuml.backend.application.auth.port.in.OAuthAuthorizeUseCase;
import com.libreuml.backend.application.auth.port.out.OAuthProviderPort;
import com.libreuml.backend.application.auth.port.out.OAuthStatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OAuthAuthorizeService implements OAuthAuthorizeUseCase {

    private final List<OAuthProviderPort> providerAdapters;
    private final OAuthStatePort statePort;

    @Override
    public String buildAuthorizationUrl(OAuthProvider provider, String redirectUri) {
        String state = statePort.generateState();
        return resolveAdapter(provider).buildAuthorizationUrl(state, redirectUri);
    }

    private OAuthProviderPort resolveAdapter(OAuthProvider provider) {
        return providerAdapters.stream()
                .filter(a -> a.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new OAuthException("No adapter registered for provider: " + provider));
    }
}
