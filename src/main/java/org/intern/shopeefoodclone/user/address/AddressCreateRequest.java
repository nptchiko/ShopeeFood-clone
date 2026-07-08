package org.intern.shopeefoodclone.user.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddressCreateRequest(
        @Size(max = 50, message = "Label must be at most 50 characters")
        String label,

        @NotBlank(message = "Line 1 is required")
        @Size(max = 255, message = "Line 1 must be at most 255 characters")
        String line1,

        @Size(max = 255, message = "Line 2 must be at most 255 characters")
        String line2,

        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @Size(max = 20, message = "Postal code must be at most 20 characters")
        String postalCode,

        Boolean isDefault,


        @Pattern(regexp = "^-?\\d{1,3}\\.\\d{1,7}$", message = "Latitude must be a valid decimal number with up to 7 decimal places")
        BigDecimal latitude,

        @Pattern(regexp = "^-?\\d{1,3}\\.\\d{1,7}$", message = "Longitude must be a valid decimal number with up to 7 decimal places")
        BigDecimal longitude
) {}
