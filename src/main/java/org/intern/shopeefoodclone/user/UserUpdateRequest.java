package org.intern.shopeefoodclone.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record UserUpdateRequest(
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Email(message = "Invalid email format")
        @Size(min = 5, max = 255, message = "Email must be at most 255 characters")
        String email,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        @Pattern(
                regexp = "^(\\+\\d{1,3}[- ]?)?\\d{10}$",
                message = "Invalid phone number format"
        )
        String phone,

        @Pattern(
                regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$",
                message = "Password must be 8-20 characters long, contain at least one digit, one uppercase letter, one lowercase letter, and one special character (@#$%^&+=!)."
        )
        String password,

        @JsonIgnore
        OffsetDateTime verifiedAt

) {}
