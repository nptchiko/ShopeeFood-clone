package org.intern.shopeefoodclone.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 50, message = "Email must be at most 50 characters")
        String email,

        @Size(max = 15, message = "Phone must be at most 15 characters")
        @Pattern(
                regexp = "^(\\+\\d{1,3}[- ]?)?\\d{10}$",
                message = "Invalid phone number format"
        )
        String phone,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
                message = "Password must be 8-20 characters long, contain at least one digit, one uppercase letter, one lowercase letter, and one special character (@#$%^&+=!)."
        )
        String password
) {}
