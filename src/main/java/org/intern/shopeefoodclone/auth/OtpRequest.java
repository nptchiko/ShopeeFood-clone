package org.intern.shopeefoodclone.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 50, message = "Email must be at most 50 characters")
        String email,

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be exactly 6 characters")
        String otp
) {}
