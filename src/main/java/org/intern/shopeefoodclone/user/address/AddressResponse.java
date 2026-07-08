package org.intern.shopeefoodclone.user.address;

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
        Double latitude,
        Double longitude) {}
