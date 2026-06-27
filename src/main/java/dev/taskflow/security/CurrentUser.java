package dev.taskflow.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

public final class CurrentUser {
    private CurrentUser() {}

    public static AuthPrincipal require() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return p;
    }

    public static void requireAdmin() {
        if (!require().isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
    }
}
