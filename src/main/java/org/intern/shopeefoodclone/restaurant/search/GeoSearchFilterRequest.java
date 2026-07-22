package org.intern.shopeefoodclone.restaurant.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record GeoSearchFilterRequest(
        @JsonProperty("min_rating")
        BigDecimal minRating,

        @JsonProperty("price_range")
        List<BigDecimal> priceRange,

        @JsonProperty("is_open_now")
        Boolean isOpenNow
) {}
