package com.appcompras.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    private final boolean requireAuth;

    public SecurityCurrentUserProvider(@Value("${app.security.require-auth:true}") boolean requireAuth) {
        this.requireAuth = requireAuth;
    }

    @Override
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt && jwt.getSubject() != null) {
            return jwt.getSubject();
        }

        if (!requireAuth) {
            return "local-dev-user";
        }

        throw new IllegalStateException("Unauthenticated request");
    }
}
