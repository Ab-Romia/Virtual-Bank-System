package com.virtualbank.common.security;

import com.virtualbank.common.web.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads the authenticated user's id from the validated JWT's {@code sub} claim.
 * The user id is never taken from a request parameter, header, or body, which is
 * what closes the IDOR holes the audit found.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static String id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return null;
    }

    public static String requireId() {
        String id = id();
        if (id == null) {
            throw ApiException.unauthorized("No authenticated user");
        }
        return id;
    }
}
