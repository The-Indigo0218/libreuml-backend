package com.libreuml.backend.application.auth.dto;

/**
 * Provider-agnostic identity record returned by any {@code OAuthProviderPort} adapter.
 *
 * <p>{@code providerId} is the stable, immutable identifier issued by the provider
 * (GitHub's numeric user ID, Google's {@code sub} claim).  It is used as the primary
 * lookup key, not the email, which can change.</p>
 *
 * <p>{@code emailVerified} gates account-linking by email: we only link an OAuth identity
 * to an existing local account when the provider has confirmed the address belongs to
 * this person, preventing account-hijacking via unverified email claims.</p>
 */
public record OAuthUserInfo(
        OAuthProvider provider,
        String providerId,
        String email,
        boolean emailVerified,
        String name,
        String avatarUrl
) {}
