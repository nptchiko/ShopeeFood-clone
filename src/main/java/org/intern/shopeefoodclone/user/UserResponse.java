package org.intern.shopeefoodclone.user;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String role
) {}
