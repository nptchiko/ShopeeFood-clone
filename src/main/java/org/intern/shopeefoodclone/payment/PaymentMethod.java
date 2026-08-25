package org.intern.shopeefoodclone.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentMethod {
    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "method_type", length = 30)
    PaymentMethodType type = PaymentMethodType.COD;

    @Column(name = "gateway_token")
    String gatewayToken;

    @Column(name = "payment_provider")
    PaymentGatewayProvider provider;

}
