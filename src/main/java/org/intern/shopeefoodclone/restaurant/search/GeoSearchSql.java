package org.intern.shopeefoodclone.restaurant.search;

/**
 * Compile-time SQL constants for PostGIS geo-search queries.
 *
 * <p>All repeated fragments — SELECT columns, the FROM/JOIN/base-WHERE clause,
 * and the common filter predicates — are defined once here. The three repository
 * query strings are then composed from these fragments.
 *
 * <p><b>Adding a new filter</b> (e.g. {@code price_range}, {@code category}):
 * <ol>
 *   <li>Append one {@code AND} line to {@link #COMMON_FILTERS}.</li>
 *   <li>Add the matching {@code @Param} to each repository method signature.</li>
 * </ol>
 * No other changes are needed — the predicate propagates to all three queries automatically.
 */
public final class GeoSearchSql {

    private GeoSearchSql() {}

    // ── Shared fragments ────────────────────────────────────────────────────

    /**
     * The 16 scalar columns returned by every geo-search query (trailing comma
     * intentional — callers append the distance expression as the final column).
     */
    private static final String SELECT_COLUMNS =
            "SELECT " +
            "r.id AS id, r.owner_user_id AS ownerId, r.name AS name, " +
            "r.address_id AS addressId, a.line1 AS addressLine1, a.line2 AS addressLine2, " +
            "a.city AS city, a.lat AS latitude, a.lng AS longitude, " +
            "r.description AS description, r.logo_url AS logoUrl, r.banner_url AS bannerUrl, " +
            "r.rating AS rating, r.is_open AS isOpen, r.created_at AS createdAt, r.updated_at AS updatedAt, ";

    /** Shared FROM / JOIN / base WHERE (geom must be present). */
    private static final String FROM_CLAUSE =
            " FROM restaurants r" +
            " JOIN addresses a ON r.address_id = a.id" +
            " WHERE a.geom IS NOT NULL";

    /**
     * Filter predicates that are identical across all three spatial strategies.
     * Each predicate is a no-op when the corresponding param is {@code null},
     * so callers never need to build conditional SQL.
     */
    private static final String COMMON_FILTERS =
            " AND (:isOpen IS NULL OR r.is_open = :isOpen)" +
            " AND (:keyword IS NULL OR :keyword = ''" +
            "      OR LOWER(r.name) LIKE CONCAT('%', LOWER(:keyword), '%')" +
            "      OR LOWER(r.description) LIKE CONCAT('%', LOWER(:keyword), '%'))" +
            " AND (:minRating IS NULL OR r.rating >= :minRating)";

    /** PostGIS expression for the distance from a point (:lat, :lng). */
    private static final String DISTANCE_EXPR =
            "ST_Distance(a.geom::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)";

    private static final String ORDER_BY_DISTANCE = " ORDER BY " + DISTANCE_EXPR + " ASC";

    // ── Nearby (radius) ─────────────────────────────────────────────────────

    private static final String NEARBY_SPATIAL_FILTER =
            " AND ST_DWithin(a.geom::geography," +
            " ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusInMeters)";

    public static final String FIND_NEARBY =
            SELECT_COLUMNS + DISTANCE_EXPR + " AS distanceInMeters" +
            FROM_CLAUSE + NEARBY_SPATIAL_FILTER + COMMON_FILTERS + ORDER_BY_DISTANCE;

    public static final String COUNT_NEARBY =
            "SELECT COUNT(r.id)" + FROM_CLAUSE + NEARBY_SPATIAL_FILTER + COMMON_FILTERS;

    // ── Bounding box ─────────────────────────────────────────────────────────

    private static final String BBOX_SPATIAL_FILTER =
            " AND ST_Within(a.geom, ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326))";

    public static final String FIND_BBOX =
            SELECT_COLUMNS + "0.0 AS distanceInMeters" +
            FROM_CLAUSE + BBOX_SPATIAL_FILTER + COMMON_FILTERS;

    public static final String COUNT_BBOX =
            "SELECT COUNT(r.id)" + FROM_CLAUSE + BBOX_SPATIAL_FILTER + COMMON_FILTERS;

    // ── By distance (no radius constraint) ───────────────────────────────────

    public static final String FIND_BY_DISTANCE =
            SELECT_COLUMNS + DISTANCE_EXPR + " AS distanceInMeters" +
            FROM_CLAUSE + COMMON_FILTERS + ORDER_BY_DISTANCE;

    public static final String COUNT_BY_DISTANCE =
            "SELECT COUNT(r.id)" + FROM_CLAUSE + COMMON_FILTERS;
}
