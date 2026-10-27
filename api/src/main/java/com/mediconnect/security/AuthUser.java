package com.mediconnect.security;

import com.mediconnect.user.Role;

/** The authenticated principal, rebuilt from the JWT on every request. */
public record AuthUser(Long id, String email, String name, Role role) {
}
