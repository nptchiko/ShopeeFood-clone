package org.intern.shopeefoodclone.user.address;

import java.math.BigDecimal;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        UUID userId,
        String label,
        String line1,
        String line2,
        String city,
        String postalCode,
        Boolean isDefault,
        BigDecimal latitude,
        BigDecimal longitude
) {}
