package org.intern.shopeefoodclone.restaurant;

import org.intern.shopeefoodclone.restaurant.search.GeoSearchSql;
import org.intern.shopeefoodclone.restaurant.search.RestaurantLocationProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID>, JpaSpecificationExecutor<Restaurant> {

    @Query(value = GeoSearchSql.FIND_NEARBY,
           countQuery = GeoSearchSql.COUNT_NEARBY,
           nativeQuery = true)
    Page<RestaurantLocationProjection> findNearby(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusInMeters") Double radiusInMeters,
            @Param("isOpen") Boolean isOpen,
            @Param("keyword") String keyword,
            @Param("minRating") BigDecimal minRating,
            Pageable pageable);

    @Query(value = GeoSearchSql.FIND_BBOX,
           countQuery = GeoSearchSql.COUNT_BBOX,
           nativeQuery = true)
    Page<RestaurantLocationProjection> findWithinBoundingBox(
            @Param("minLat") Double minLat,
            @Param("minLng") Double minLng,
            @Param("maxLat") Double maxLat,
            @Param("maxLng") Double maxLng,
            @Param("isOpen") Boolean isOpen,
            @Param("keyword") String keyword,
            @Param("minRating") BigDecimal minRating,
            Pageable pageable);

    @Query(value = GeoSearchSql.FIND_BY_DISTANCE,
           countQuery = GeoSearchSql.COUNT_BY_DISTANCE,
           nativeQuery = true)
    Page<RestaurantLocationProjection> findAllSortedByDistance(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("isOpen") Boolean isOpen,
            @Param("keyword") String keyword,
            @Param("minRating") BigDecimal minRating,
            Pageable pageable);
}
