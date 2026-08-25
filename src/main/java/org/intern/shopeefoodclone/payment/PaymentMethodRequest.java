package org.intern.shopeefoodclone.payment;

import jakarta.validation.constraints.NotNull;

public record PaymentMethodRequest(
        @NotNull(message = "Payment method type is required")
        PaymentMethodType type,

        PaymentGatewayProvider provider,

        String gatewayToken
) {
        
}
