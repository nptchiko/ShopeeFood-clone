# Restaurant Geo-Search — Software Requirements Specification

| Field       | Value                                  |
|-------------|----------------------------------------|
| **Project** | ShopeeFood Clone                       |
| **Module**  | Restaurant — Geo-Search Sub-feature    |
| **File**    | `geosearch-spec.md`                    |
| **Version** | 1.0.0                                  |
| **Date**    | 2026-07-22                             |
| **Status**  | Implemented                            |

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Overall Description](#2-overall-description)
3. [Architecture](#3-architecture)
4. [Data Models](#4-data-models)
5. [Functional Requirements](#5-functional-requirements)
6. [Business Rules & Constraints](#6-business-rules--constraints)
7. [Error Catalogue](#7-error-catalogue)
8. [API Specification](#8-api-specification)
9. [Request Body Reference](#9-request-body-reference)
10. [Non-Functional Requirements](#10-non-functional-requirements)

---

## 1. Introduction

### 1.1 Purpose

This document defines the requirements and API contract for the **Restaurant Geo-Search**
sub-feature of the ShopeeFood Clone backend. It is the single source of truth for
developers, testers, and stakeholders for this capability.

### 1.2 Scope

The geo-search feature exposes three spatial search strategies, all accessible via
`POST` endpoints under `/api/restaurants/_search/*`:

| Strategy | Endpoint | Description |
|---|---|---|
| **Nearby** | `/_search/nearby` | Find restaurants within a radius of a point |
| **Bounding Box** | `/_search/bounding-box` | Find restaurants within a lat/lng rectangle |
| **By Distance** | `/_search/by-distance` | Return all restaurants sorted by distance from a point |

All three strategies share a unified request body schema and support the same set of
optional filter predicates.

### 1.3 Technology Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Persistence | Spring Data JPA / Hibernate + PostgreSQL |
| Spatial | PostGIS (`ST_DWithin`, `ST_Within`, `ST_MakeEnvelope`, `ST_Distance`) |
| Projection | JPA Interface Projections (`RestaurantLocationProjection`) |
| Auth | JWT Bearer token (Spring Security) |
| Validation | Jakarta Bean Validation |

---

## 2. Overall Description

### 2.1 Why POST for Search?

The geo-search endpoints use `POST` rather than `GET` to keep the URL short and structured.
Sending coordinates, filters, and pagination as query params would produce excessively long
URLs that are hard to read, cache, and proxy safely. The body is always JSON.

> [!NOTE]
> Pagination (`page`, `size`, `sort`) is still passed as standard **query parameters** on
> the URL (resolved by Spring's `Pageable` infrastructure), consistent with every other
> paginated endpoint in the system.

### 2.2 Request Processing Pipeline

```
POST /_search/{strategy}
        │
        ▼
RestaurantController          ← deserialises JSON body into RestaurantSearchRequest
        │ RestaurantSearchQuery.from(request, pageable)
        ▼
RestaurantSearchQuery         ← resolved primitives (no JSON-binding knowledge)
        │
        ▼
RestaurantService             ← validates coordinates/radius; calls repository
        │
        ▼
RestaurantRepository          ← executes native PostGIS SQL via @Query(GeoSearchSql.*)
        │
        ▼
RestaurantLocationProjection  ← flat JPA projection including distanceInMeters
        │
        ▼
RestaurantSearchResponse      ← JSON response with restaurant data + distance
```

### 2.3 URL Namespace

| Operation | Method | Path |
|---|---|---|
| Nearby radius search | POST | `/api/restaurants/_search/nearby` |
| Bounding box search | POST | `/api/restaurants/_search/bounding-box` |
| Sort by distance | POST | `/api/restaurants/_search/by-distance` |

---

## 3. Architecture

### 3.1 Layer Responsibilities

| Class | Package | Role |
|---|---|---|
| `RestaurantController` | `restaurant` | HTTP entry point; deserialises body; translates to query object |
| `RestaurantSearchRequest` | `restaurant.search` | Pure JSON container — no business logic |
| `RestaurantSearchQuery` | `restaurant.search` | Resolved primitives + `Pageable`; built by `from()` factory |
| `RestaurantService` | `restaurant` | Validates inputs; dispatches to repository |
| `GeoSearchSql` | `restaurant.search` | Compile-time SQL constants; single source of truth for query strings |
| `RestaurantRepository` | `restaurant` | Spring Data JPA repository; native PostGIS queries |
| `RestaurantLocationProjection` | `restaurant.search` | JPA interface projection mapping SQL result columns |
| `RestaurantSearchResponse` | `restaurant.search` | API response record; assembled from projection |

### 3.2 Request Body Design — Two-Tier Structure

The JSON body is intentionally split into two top-level concerns:

```
RestaurantSearchRequest
├── location   (GeoSearchLocationRequest)   — where to search
├── filters    (GeoSearchFilterRequest)     — what to include/exclude
├── query      (String)                     — keyword search term
└── sort_by    (String)                     — sort strategy hint
```

This means the service layer **never imports or references** `GeoSearchFilterRequest` or
`GeoSearchLocationRequest` directly — it only sees resolved primitive values via
`RestaurantSearchQuery`. Adding a new filter field only requires changes to
`GeoSearchFilterRequest` and `RestaurantSearchQuery.from()`.

### 3.3 SQL Composition

All three repository queries are composed from shared SQL fragments in `GeoSearchSql`:

```
FIND_NEARBY        = SELECT_COLUMNS + DISTANCE_EXPR + FROM_CLAUSE + NEARBY_SPATIAL_FILTER + COMMON_FILTERS + ORDER_BY_DISTANCE
FIND_BBOX          = SELECT_COLUMNS + "0.0"         + FROM_CLAUSE + BBOX_SPATIAL_FILTER   + COMMON_FILTERS
FIND_BY_DISTANCE   = SELECT_COLUMNS + DISTANCE_EXPR + FROM_CLAUSE                         + COMMON_FILTERS + ORDER_BY_DISTANCE
```

| Fragment | Content |
|---|---|
| `SELECT_COLUMNS` | 16 scalar columns shared by all three queries |
| `FROM_CLAUSE` | `FROM restaurants r JOIN addresses a … WHERE a.geom IS NOT NULL` |
| `COMMON_FILTERS` | `isOpen`, `keyword`, `minRating` predicates — the only place to add new filters |
| `NEARBY_SPATIAL_FILTER` | `ST_DWithin(…, :radiusInMeters)` |
| `BBOX_SPATIAL_FILTER` | `ST_Within(…, ST_MakeEnvelope(…))` |
| `DISTANCE_EXPR` | `ST_Distance(…)` — used in both SELECT and ORDER BY |

---

## 4. Data Models

### 4.1 GeoSearchLocationRequest

Fields sent inside the `"location"` object:

| JSON Field | Type | Required by Strategy | Description |
|---|---|---|---|
| `latitude` | Double | nearby, by-distance | Reference point latitude (`-90` to `90`) |
| `longitude` | Double | nearby, by-distance | Reference point longitude (`-180` to `180`) |
| `radius_in_meters` | Double | nearby | Search radius in metres (1–50 000). Defaults to `5000` if omitted. |
| `min_latitude` | Double | bounding-box | Southern boundary |
| `min_longitude` | Double | bounding-box | Western boundary |
| `max_latitude` | Double | bounding-box | Northern boundary |
| `max_longitude` | Double | bounding-box | Eastern boundary |

### 4.2 GeoSearchFilterRequest

Fields sent inside the `"filters"` object:

| JSON Field | Type | Required | Description |
|---|---|---|---|
| `is_open_now` | Boolean | No | If `true`, only open restaurants are returned |
| `min_rating` | BigDecimal | No | Minimum inclusive rating threshold |
| `price_range` | List\<BigDecimal\> | No | *(Defined; not yet wired to SQL)* |

### 4.3 RestaurantSearchResponse

Each item in the paginated `content` array:

| Field | Type | Description |
|---|---|---|
| `restaurant` | RestaurantResponse | Core restaurant fields (id, name, rating, isOpen, etc.) |
| `address_line_1` | String | Street address line 1 |
| `address_line_2` | String | Street address line 2 |
| `city` | String | City |
| `latitude` | BigDecimal | Restaurant's geographic latitude |
| `longitude` | BigDecimal | Restaurant's geographic longitude |
| `distance_in_meters` | Double | Distance from the reference point, rounded to 1 decimal place. `null` for bounding-box results. |

---

## 5. Functional Requirements

| ID | Requirement |
|---|---|
| GS-01 | The system **shall** accept a structured JSON body for all geo-search endpoints via `POST`. |
| GS-02 | The system **shall** support searching restaurants within a circular radius of a given lat/lng point (`nearby`). |
| GS-03 | The system **shall** support searching restaurants within a rectangular bounding box defined by `minLat`, `minLng`, `maxLat`, `maxLng` (`bounding-box`). |
| GS-04 | The system **shall** support returning all restaurants sorted by ascending distance from a given lat/lng point (`by-distance`). |
| GS-05 | All three strategies **shall** support optional keyword filtering against restaurant `name` and `description` (case-insensitive, partial match). |
| GS-06 | All three strategies **shall** support optional `is_open_now` filtering. |
| GS-07 | All three strategies **shall** support optional `min_rating` filtering. |
| GS-08 | All results **shall** be returned as a paginated `PageResponse`, controlled by `page`, `size`, and `sort` query parameters. |
| GS-09 | The `distance_in_meters` field in the response **shall** be rounded to one decimal place. |
| GS-10 | For the `nearby` strategy, the system **shall** apply a default radius of `5000` metres when `radius_in_meters` is omitted from the request. |
| GS-11 | Restaurants without a linked address (and therefore without a PostGIS `geom` column value) **shall** be excluded from all geo-search results. |
| GS-12 | The system **shall** reject the request with `INVALID_INPUT` when required coordinate fields are absent or out of valid range. |
| GS-13 | Pagination page size **shall** be bounded server-side (via `PaginationUtils`) to prevent unbounded queries. |

---

## 6. Business Rules & Constraints

### 6.1 Coordinate Validation

| Rule | Constraint |
|---|---|
| **BR-GS-01** | `latitude` must be in range `[-90, 90]` |
| **BR-GS-02** | `longitude` must be in range `[-180, 180]` |
| **BR-GS-03** | `radius_in_meters` must be in range `(0, 50 000]`; defaults to `5000` if absent |
| **BR-GS-04** | For bounding box: `minLat < maxLat` AND `minLng < maxLng` must hold |
| **BR-GS-05** | For bounding box: all four corners must be within valid lat/lng ranges |

### 6.2 Filter Behaviour

| Rule | Constraint |
|---|---|
| **BR-GS-06** | All filter fields are optional — omitting them returns unfiltered results for that dimension |
| **BR-GS-07** | Keyword matching is **case-insensitive** and applied to both `name` and `description` using SQL `LIKE CONCAT('%', LOWER(:keyword), '%')` |
| **BR-GS-08** | `min_rating` is **inclusive** (`>=`) |
| **BR-GS-09** | `is_open_now` filters on the `is_open` boolean column — it reflects the restaurant's manually-toggled status, not real-time hours |

### 6.3 Coordinate Reference System

| Rule | Value |
|---|---|
| **BR-GS-10** | All geometry operations use **SRID 4326** (WGS 84, standard GPS coordinates) |
| **BR-GS-11** | Distance calculations use **PostGIS geography** type (`::geography` cast) for metre-accurate great-circle distances |
| **BR-GS-12** | Bounding box uses the **geometry** type (`ST_Within` / `ST_MakeEnvelope`) — appropriate for rectangular viewport filtering |

---

## 7. Error Catalogue

| Error Code | HTTP | Constant | Trigger |
|---|---|---|---|
| `40000` | 400 | `INVALID_INPUT` | Missing required coordinates, out-of-range values, invalid radius |

### 7.1 Validation Error Messages

| Condition | Message |
|---|---|
| `latitude` or `longitude` missing (nearby / by-distance) | `"Latitude and longitude must be provided for nearby search."` |
| `latitude` or `longitude` missing (by-distance) | `"Latitude and longitude must be provided."` |
| Coordinates out of range | `"Invalid coordinates provided."` |
| `radius_in_meters` out of range | `"Radius must be between 1 and 50000 meters."` |
| Any bounding box corner missing | `"All four bounding box coordinates (minLat, minLng, maxLat, maxLng) must be provided."` |
| Bounding box geometrically invalid | `"Invalid bounding box coordinates provided."` |

---

## 8. API Specification

### Conventions

- **Base URL:** `http://localhost:8080`
- **Auth:** `Authorization: Bearer <jwt>` header (where required)
- **Content-Type:** `application/json`
- **Pagination:** via query params — `page` (0-indexed), `size`, `sort`

---

### 8.1 `POST /api/restaurants/_search/nearby`

Find restaurants within a circular radius from a reference point.

**Required fields:** `location.latitude`, `location.longitude`
**Default:** `location.radius_in_meters` → `5000` m if omitted

**Request Body**

```json
{
  "location": {
    "latitude": 10.762622,
    "longitude": 106.660172,
    "radius_in_meters": 3000
  },
  "filters": {
    "is_open_now": true,
    "min_rating": 4.0
  },
  "query": "bún bò",
  "sort_by": "distance"
}
```

**Validation**

| Field | Rule |
|---|---|
| `location.latitude` | Required; in `[-90, 90]` |
| `location.longitude` | Required; in `[-180, 180]` |
| `location.radius_in_meters` | Optional; in `(0, 50000]`; default `5000` |

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Nearby restaurants retrieved successfully",
  "data": {
    "content": [
      {
        "restaurant": {
          "id": "uuid",
          "ownerId": "uuid",
          "name": "Bún Bò Huế Mẹ Ghẻ",
          "addressId": "uuid",
          "description": "Authentic Central Vietnamese spicy noodle soup",
          "logoUrl": "https://cdn.example.com/logo.png",
          "bannerUrl": "https://cdn.example.com/banner.jpg",
          "rating": 4.50,
          "isOpen": true,
          "createdAt": "2026-07-01T08:00:00+07:00",
          "updatedAt": "2026-07-22T01:00:00+07:00"
        },
        "address_line_1": "123 Nguyễn Trãi",
        "address_line_2": "Phường 2",
        "city": "Hồ Chí Minh",
        "latitude": 10.7626,
        "longitude": 106.6602,
        "distance_in_meters": 284.5
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

**Results are ordered:** by `distance_in_meters` ascending (closest first), always.

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| `latitude` or `longitude` missing | `40000` | 400 |
| Coordinates out of range | `40000` | 400 |
| `radius_in_meters` ≤ 0 or > 50 000 | `40000` | 400 |

---

### 8.2 `POST /api/restaurants/_search/bounding-box`

Find restaurants whose address falls within a geographic rectangle (viewport).

**Required fields:** `location.min_latitude`, `location.min_longitude`, `location.max_latitude`, `location.max_longitude`

**Request Body**

```json
{
  "location": {
    "min_latitude": 10.750000,
    "min_longitude": 106.640000,
    "max_latitude": 10.780000,
    "max_longitude": 106.680000
  },
  "filters": {
    "is_open_now": true,
    "min_rating": 3.5
  },
  "query": "pizza"
}
```

**Validation**

| Field | Rule |
|---|---|
| `location.min_latitude` | Required; in `[-90, 90]` |
| `location.min_longitude` | Required; in `[-180, 180]` |
| `location.max_latitude` | Required; in `[-90, 90]`; must be > `min_latitude` |
| `location.max_longitude` | Required; in `[-180, 180]`; must be > `min_longitude` |

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Restaurants within bounding box retrieved successfully",
  "data": {
    "content": [
      {
        "restaurant": { "...": "..." },
        "address_line_1": "45 Lê Lợi",
        "address_line_2": null,
        "city": "Hồ Chí Minh",
        "latitude": 10.7654,
        "longitude": 106.6512,
        "distance_in_meters": null
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 8,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

> [!NOTE]
> `distance_in_meters` is always `null` for bounding-box results — the strategy does not
> compute distance from a reference point (there is none).

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Any bounding box corner missing | `40000` | 400 |
| `minLat >= maxLat` or `minLng >= maxLng` | `40000` | 400 |
| Any corner out of valid range | `40000` | 400 |

---

### 8.3 `POST /api/restaurants/_search/by-distance`

Return **all** restaurants (no spatial boundary) sorted by ascending distance from a point.
Useful for "show me everything nearby, closest first" with no hard cutoff.

**Required fields:** `location.latitude`, `location.longitude`

**Request Body**

```json
{
  "location": {
    "latitude": 10.762622,
    "longitude": 106.660172
  },
  "filters": {
    "is_open_now": false,
    "min_rating": 4.5
  }
}
```

**Validation**

| Field | Rule |
|---|---|
| `location.latitude` | Required; in `[-90, 90]` |
| `location.longitude` | Required; in `[-180, 180]` |

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Restaurants sorted by distance retrieved successfully",
  "data": {
    "content": [
      {
        "restaurant": { "...": "..." },
        "address_line_1": "78 Trần Hưng Đạo",
        "address_line_2": "Quận 1",
        "city": "Hồ Chí Minh",
        "latitude": 10.7630,
        "longitude": 106.6610,
        "distance_in_meters": 112.3
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 54,
    "totalPages": 3,
    "first": true,
    "last": false
  }
}
```

**Results are ordered:** by `distance_in_meters` ascending.

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| `latitude` or `longitude` missing | `40000` | 400 |
| Coordinates out of range | `40000` | 400 |

---

## 9. Request Body Reference

### 9.1 Full Schema

```
RestaurantSearchRequest
│
├── location  (object, required for all strategies)
│   ├── latitude          Double   — reference lat  (nearby, by-distance)
│   ├── longitude         Double   — reference lng  (nearby, by-distance)
│   ├── radius_in_meters  Double   — radius in m    (nearby only; default 5000)
│   ├── min_latitude      Double   — bbox south     (bounding-box)
│   ├── min_longitude     Double   — bbox west      (bounding-box)
│   ├── max_latitude      Double   — bbox north     (bounding-box)
│   └── max_longitude     Double   — bbox east      (bounding-box)
│
├── filters  (object, optional)
│   ├── is_open_now       Boolean     — filter by open status
│   ├── min_rating        BigDecimal  — minimum rating (inclusive)
│   └── price_range       List<BigDecimal>  — (reserved, not yet wired)
│
├── query    String   — keyword search against name + description
└── sort_by  String   — hint field (e.g. "distance", "rating"); reserved for future use
```

### 9.2 Query Parameters (Pagination)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | int | `0` | Zero-based page index |
| `size` | int | `20` | Page size (server-bounded) |
| `sort` | string | — | Field and direction, e.g. `rating,desc` |

### 9.3 Minimal Request Examples

**Nearby — minimal (lat/lng only, radius defaults to 5 km):**
```json
{
  "location": { "latitude": 10.762622, "longitude": 106.660172 }
}
```

**Bounding box — minimal:**
```json
{
  "location": {
    "min_latitude": 10.75, "min_longitude": 106.64,
    "max_latitude": 10.78, "max_longitude": 106.68
  }
}
```

**By distance — with keyword and rating filter:**
```json
{
  "location": { "latitude": 10.762622, "longitude": 106.660172 },
  "filters": { "min_rating": 4.0, "is_open_now": true },
  "query": "cơm tấm"
}
```

---

## 10. Non-Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-GS-01 | **Performance** | PostGIS `geom` column on `addresses` **must** have a spatial index (`GIST`). Without it, `ST_DWithin` and `ST_Within` degrade to full table scans. |
| NFR-GS-02 | **Accuracy** | Distance calculations use PostGIS `geography` type, providing metre-accurate great-circle distance (Haversine). |
| NFR-GS-03 | **Pagination** | All geo-search endpoints enforce a server-side page-size cap via `PaginationUtils.validateAndBound()`. |
| NFR-GS-04 | **Transactions** | All three search methods run in `@Transactional(readOnly = true)`. |
| NFR-GS-05 | **Extensibility** | Adding a new filter field requires changes to `GeoSearchFilterRequest`, `GeoSearchSql.COMMON_FILTERS`, and the three repository method `@Param` signatures only. The service layer and controller require no changes. |
| NFR-GS-06 | **Exclusion** | Restaurants without a valid PostGIS geometry (`a.geom IS NOT NULL`) are silently excluded from all results — no error is raised. |
| NFR-GS-07 | **API Documentation** | All three endpoints are discoverable via Swagger/OpenAPI UI at `/swagger-ui.html`. |
