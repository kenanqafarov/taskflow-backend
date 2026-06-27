package dev.taskflow.security;

import java.util.UUID;

public record AuthPrincipal(UUID id, String username, String role) {
    public boolean isAdmin() { return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role); }
}
