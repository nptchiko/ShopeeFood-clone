package org.intern.shopeefoodclone.restaurant.search;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Resolved, validated query object built from a {@link RestaurantSearchRequest}.
 * <p>
 * This is the object the service layer works with. All fields are already resolved
 * to their final primitive values — there is no nested-vs-flat ambiguity here.
 * Adding a new filter means touching {@link GeoSearchFilterRequest} and the
 * {@link #from} factory only; the service signature stays stable.
 */
public record RestaurantSearchQuery(
        Double latitude,
        Double longitude,
        Double radiusInMeters,
        Double minLatitude,
        Double minLongitude,
        Double maxLatitude,
        Double maxLongitude,
        String keyword,
        Boolean isOpen,
        BigDecimal minRating,
        Pageable pageable
) {

    /**
     * Builds a {@code RestaurantSearchQuery} from the raw request body and
     * the pageable resolved by Spring from query params.
     *
     * <p>Resolution rules (applied per field):
     * <ol>
     *   <li>Use the value from the nested {@code location} / {@code filters} object if present.</li>
     *   <li>Otherwise leave {@code null} (callers validate what they need).</li>
     * </ol>
     */
    public static RestaurantSearchQuery from(RestaurantSearchRequest request, Pageable pageable) {
        GeoSearchLocationRequest loc = request.location();
        GeoSearchFilterRequest fil = request.filters();

        String keyword = null;
        if (request.query() != null && !request.query().isBlank()) {
            keyword = request.query().trim();
        }

        return new RestaurantSearchQuery(
                loc != null ? loc.latitude()       : null,
                loc != null ? loc.longitude()      : null,
                loc != null ? loc.radiusInMeters() : null,
                loc != null ? loc.minLatitude()    : null,
                loc != null ? loc.minLongitude()   : null,
                loc != null ? loc.maxLatitude()    : null,
                loc != null ? loc.maxLongitude()   : null,
                keyword,
                fil != null ? fil.isOpenNow()  : null,
                fil != null ? fil.minRating()  : null,
                pageable
        );
    }
}
