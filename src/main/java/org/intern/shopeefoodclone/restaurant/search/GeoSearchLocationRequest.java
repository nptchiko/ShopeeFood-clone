package org.intern.shopeefoodclone.restaurant.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeoSearchLocationRequest(
        @JsonProperty("latitude")
        Double latitude,

        @JsonProperty("longitude")
        Double longitude,

        @JsonProperty("radius_in_meters")
        Double radiusInMeters,

        @JsonProperty("min_latitude")
        Double minLatitude,

        @JsonProperty("min_longitude")
        Double minLongitude,

        @JsonProperty("max_latitude")
        Double maxLatitude,

        @JsonProperty("max_longitude")
        Double maxLongitude
) {}
