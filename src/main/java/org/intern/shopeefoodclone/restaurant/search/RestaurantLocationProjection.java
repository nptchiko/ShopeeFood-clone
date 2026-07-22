package org.intern.shopeefoodclone.restaurant.search;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projection interface for native PostGIS geospatial search queries on restaurants.
 * Maps columns returned by ST_Distance / ST_DWithin / ST_MakeEnvelope queries.
 * Reference: https://neon.com/guides/geospatial-search
 */
public interface RestaurantLocationProjection {
    UUID getId();
    UUID getOwnerId();
    String getName();
    UUID getAddressId();
    String getAddressLine1();
    String getAddressLine2();
    String getCity();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    String getDescription();
    String getLogoUrl();
    String getBannerUrl();
    BigDecimal getRating();
    Boolean getIsOpen();
    java.time.Instant getCreatedAt();
    java.time.Instant getUpdatedAt();
    Double getDistanceInMeters();
}

