package org.intern.shopeefoodclone.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String role
) {}
