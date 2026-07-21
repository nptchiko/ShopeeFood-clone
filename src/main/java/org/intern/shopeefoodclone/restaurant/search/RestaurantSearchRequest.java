package org.intern.shopeefoodclone.restaurant.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw POST body for restaurant geo-search endpoints.
 * <p>
 * This record is a pure JSON container — it only knows about deserialization.
 * No resolver methods, no flat-field fallbacks, no business logic live here.
 * <p>
 * To get a ready-to-use query object, call
 * {@link RestaurantSearchQuery#from(RestaurantSearchRequest, org.springframework.data.domain.Pageable)}.
 *
 * <p><b>Accepted body shape:</b>
 * <pre>{@code
 * {
 *   "location": {
 *     "latitude": 10.762622,
 *     "longitude": 106.660172,
 *     "radius_in_meters": 3000
 *   },
 *   "filters": {
 *     "is_open_now": true,
 *     "min_rating": 4.0
 *   },
 *   "query": "bun bo",
 *   "sort_by": "rating"
 * }
 * }</pre>
 */
public record RestaurantSearchRequest(
        @JsonProperty("location")
        GeoSearchLocationRequest location,

        @JsonProperty("filters")
        GeoSearchFilterRequest filters,

        @JsonProperty("query")
        @JsonAlias({"query", "keyword"})
        String query,

        @JsonProperty("sort_by")
        @JsonAlias({"sort_by", "sortBy"})
        String sortBy
) {}
