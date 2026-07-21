package org.intern.shopeefoodclone.restaurant.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.intern.shopeefoodclone.restaurant.RestaurantResponse;
import org.intern.shopeefoodclone.shared.utils.DateUtils;

import java.math.BigDecimal;

/**
 * Response record representing a restaurant found via PostGIS geospatial search,
 * including its distance in meters from the reference point.
 */


@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RestaurantSearchResponse(
        RestaurantResponse restaurant,
        @JsonProperty("address_line_1")
        String addressLine1,

        @JsonProperty("address_line_2")
        String addressLine2,
        String city,
        BigDecimal latitude,
        BigDecimal longitude,
        @JsonProperty("distance_in_meters")
        Double distanceInMeters
) {
    public static RestaurantSearchResponse fromProjection(RestaurantLocationProjection projection) {
        if (projection == null) {
            return null;
        }
        Double formattedDistance = projection.getDistanceInMeters() != null
                ? Math.round(projection.getDistanceInMeters() * 10.0) / 10.0
                : null;
        RestaurantResponse restaurant = new RestaurantResponse(
                projection.getId(),
                projection.getOwnerId(),
                projection.getName(),
                projection.getAddressId(),
                projection.getDescription(),
                projection.getLogoUrl(),
                projection.getBannerUrl(),
                projection.getRating(),
                projection.getIsOpen(),
                DateUtils.toOffsetDateTime(projection.getCreatedAt()),
                DateUtils.toOffsetDateTime(projection.getUpdatedAt())
        );
        return new RestaurantSearchResponse(
                restaurant,
                projection.getAddressLine1(),
                projection.getAddressLine2(),
                projection.getCity(),
                projection.getLatitude(),
                projection.getLongitude(),
                formattedDistance
        );
    }
}
