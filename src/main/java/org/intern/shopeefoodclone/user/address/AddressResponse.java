package org.intern.shopeefoodclone.user.address;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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
