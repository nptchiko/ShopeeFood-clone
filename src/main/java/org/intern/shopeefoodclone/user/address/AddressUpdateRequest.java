package org.intern.shopeefoodclone.user.address;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record AddressUpdateRequest(
        @Size(max = 50, message = "Label must be at most 50 characters")
        String label,

        @Size(max = 255, message = "Line 1 must be at most 255 characters")
        String line1,

        @Size(max = 255, message = "Line 2 must be at most 255 characters")
        String line2,

        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @Size(max = 20, message = "Postal code must be at most 20 characters")
        String postalCode,

        Boolean isDefault,

        @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0",   message = "Latitude must be <= 90")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
        Double longitude
) {}
