package org.intern.shopeefoodclone.user.address;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.intern.shopeefoodclone.user.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Size(max = 50)
    @Column(length = 50)
    private String label; // HOME, WORK, etc.

    @NotBlank(message = "Line 1 is required")
    @Size(max = 255)
    @Column(nullable = false)
    private String line1;

    @Size(max = 255)
    @Column
    private String line2;

    @Size(max = 100)
    @Column(length = 100)
    private String city;

    @Size(max = 20)
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "lat", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "lng", precision = 10, scale = 7)
    private BigDecimal longitude;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
