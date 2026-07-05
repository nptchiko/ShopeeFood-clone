package org.intern.shopeefoodclone.auth.otp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 50, message = "Email must be at most 50 characters")
        String email,

        String otp
) {}
