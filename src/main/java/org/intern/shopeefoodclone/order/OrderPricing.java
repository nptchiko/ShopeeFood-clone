package org.intern.shopeefoodclone.order;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_pricings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderPricing {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @NotNull(message = "Subtotal is required")
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal subtotal;

    @Builder.Default
    @Column(name = "delivery_fee", precision = 10, scale = 2)
    BigDecimal deliveryFee = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "platform_fee", precision = 10, scale = 2)
    BigDecimal platformFee = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "discount_amount", precision = 10, scale = 2)
    BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @NotNull(message = "Total amount is required")
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    BigDecimal totalAmount = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;
}
