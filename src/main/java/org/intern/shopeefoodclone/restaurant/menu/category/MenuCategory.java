package org.intern.shopeefoodclone.restaurant.menu.category;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.intern.shopeefoodclone.restaurant.Restaurant;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "menu_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
