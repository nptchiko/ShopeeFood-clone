# Software Requirements Specification
## Restaurant Module — ShopeeFood Clone

| Field | Value |
|---|---|
| **Project** | ShopeeFood Clone |
| **Module** | Restaurant (incl. Menu Categories & Menu Items) |
| **Version** | 1.0.0 |
| **Date** | 2026-07-07 |
| **Status** | Draft |

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Overall Description](#2-overall-description)
3. [Data Models](#3-data-models)
4. [Functional Requirements](#4-functional-requirements)
5. [Business Rules](#5-business-rules)
6. [Error Catalogue](#6-error-catalogue)
7. [API Specification](#7-api-specification)
8. [Filtering Reference (RSQL)](#8-filtering-reference-rsql)
9. [Non-Functional Requirements](#9-non-functional-requirements)

---

## 1. Introduction

### 1.1 Purpose
This document defines the software requirements for the **Restaurant** module of the ShopeeFood Clone backend. It serves as the single source of truth for developers, testers, and stakeholders regarding the capabilities, constraints, and API contract of this module.

### 1.2 Scope
The module covers three tightly coupled sub-modules:

| Sub-module | Responsibility |
|---|---|
| **Restaurant** | CRUD for restaurant profiles; RSQL-based list filtering |
| **Menu Category** | Organises menu items into named, ordered groups per restaurant |
| **Menu Item** | Individual food/beverage products inside a category |

### 1.3 Technology Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Persistence | Spring Data JPA / Hibernate + PostgreSQL |
| Filtering | `io.github.perplexhub:rsql-jpa-spring-boot-starter:7.0.2` |
| API Style | RESTful JSON |
| Auth | JWT Bearer token (Spring Security) |
| Validation | Jakarta Bean Validation |

---

## 2. Overall Description

### 2.1 Entity Hierarchy

```
Restaurant
└── MenuCategory  (1 restaurant → many categories)
    └── MenuItem  (1 category → many items)
```

### 2.2 URL Namespace

| Resource | Base Path |
|---|---|
| Restaurants | `/api/restaurants` |
| Menu Categories (scoped) | `/api/restaurants/{restaurantId}/categories` |
| Menu Categories (direct) | `/api/categories/{id}` |
| Menu Items (scoped) | `/api/categories/{categoryId}/items` |
| Menu Items (direct) | `/api/items/{id}` |

### 2.3 Response Envelope

All endpoints return a common JSON envelope:

```json
{
  "status": 200,
  "message": "Human-readable result message",
  "data": { ... }
}
```

For paginated responses the `data` field is a `PageResponse` object:

```json
{
  "status": 200,
  "message": "...",
  "data": {
    "content": [ ... ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 42,
    "totalPages": 3,
    "first": true,
    "last": false
  }
}
```

---

## 3. Data Models

### 3.1 Restaurant

| Field | DB Column | Type | Constraints | Notes |
|---|---|---|---|---|
| `id` | `id` | UUID | PK, auto-generated | |
| `owner` | `owner_user_id` | UUID (FK → `users`) | NOT NULL | Eager-loaded |
| `name` | `name` | VARCHAR(150) | NOT NULL, max 150 | |
| `address` | `address_id` | UUID (FK → `addresses`) | nullable | Eager-loaded |
| `description` | `description` | TEXT | nullable | |
| `logoUrl` | `logo_url` | TEXT | nullable | |
| `bannerUrl` | `banner_url` | TEXT | nullable | |
| `rating` | `rating` | NUMERIC(3,2) | nullable, default `0.00` | Manually updated |
| `isOpen` | `is_open` | BOOLEAN | nullable, default `true` | |
| `createdAt` | `created_at` | TIMESTAMPTZ | NOT NULL, immutable, auto-set | |
| `updatedAt` | `updated_at` | TIMESTAMPTZ | NOT NULL, auto-updated | |

### 3.2 MenuCategory

| Field | DB Column | Type | Constraints | Notes |
|---|---|---|---|---|
| `id` | `id` | UUID | PK, auto-generated | |
| `restaurant` | `restaurant_id` | UUID (FK → `restaurants`) | NOT NULL | Lazy-loaded |
| `name` | `name` | VARCHAR(100) | NOT NULL, max 100 | Unique per restaurant |
| `sortOrder` | `sort_order` | INTEGER | nullable, default `0` | Controls display order |
| `createdAt` | `created_at` | TIMESTAMPTZ | NOT NULL, immutable | |

### 3.3 MenuItem

| Field | DB Column | Type | Constraints | Notes |
|---|---|---|---|---|
| `id` | `id` | UUID | PK, auto-generated | |
| `category` | `category_id` | UUID (FK → `menu_categories`) | NOT NULL | Lazy-loaded |
| `name` | `name` | VARCHAR(150) | NOT NULL, max 150 | Unique per category |
| `description` | `description` | TEXT | nullable | |
| `price` | `price` | NUMERIC(10,2) | NOT NULL, > 0 | |
| `imageUrl` | `image_url` | TEXT | nullable | |
| `isAvailable` | `is_available` | BOOLEAN | nullable | |
| `createdAt` | `created_at` | TIMESTAMPTZ | NOT NULL, immutable | |
| `updatedAt` | `updated_at` | TIMESTAMPTZ | NOT NULL, auto-updated | |

---

## 4. Functional Requirements

### 4.1 Restaurant

| ID | Requirement |
|---|---|
| R-01 | The system **shall** allow creation of a restaurant by supplying an `ownerId`, `name`, and optional metadata fields. |
| R-02 | On creation, `rating` defaults to `0.00` if not provided; `isOpen` defaults to `true` if not provided. |
| R-03 | The system **shall** return a paginated list of restaurants, supporting RSQL-based filtering via a `filter` query parameter. |
| R-04 | Page size **shall** be bounded by a server-side maximum (default 20). |
| R-05 | The system **shall** return a restaurant by its UUID, including the full ordered menu (all categories with their items). |
| R-06 | The system **shall** allow updating any combination of: `name`, `rating`, `addressId`, `description`, `logoUrl`, `bannerUrl`, `isOpen`. |
| R-07 | On update, if `addressId` changes, the system validates the address exists before linking it. |
| R-08 | Deleting a restaurant **shall** cascade-delete all its menu categories and their items. |

### 4.2 Menu Category

| ID | Requirement |
|---|---|
| C-01 | The system **shall** allow creation of a menu category under a given restaurant. |
| C-02 | Category `name` **shall** be unique within a restaurant (case-sensitive). |
| C-03 | `sortOrder` defaults to `0` if not provided. |
| C-04 | The system **shall** return a paginated list of categories for a restaurant, with optional keyword search and embedded items. |
| C-05 | The system **shall** return a single category by UUID with its embedded item list. |
| C-06 | The system **shall** allow updating `name` and/or `sortOrder`. Uniqueness constraint is re-checked on name change. |
| C-07 | Deleting a category **shall** cascade-delete all its menu items. |

### 4.3 Menu Item

| ID | Requirement |
|---|---|
| I-01 | The system **shall** allow creation of a menu item under a given category. |
| I-02 | Item `name` **shall** be unique within a category (case-sensitive). |
| I-03 | `price` must be strictly greater than `0`. |
| I-04 | The system **shall** return a paginated list of items for a category, with optional filters: `keyword`, `minPrice`, `maxPrice`, `isAvailable`. |
| I-05 | The system **shall** return a single item by UUID. |
| I-06 | The system **shall** allow partial updates of all item fields. |
| I-07 | The system **shall** allow deletion of an individual item. |

---

## 5. Business Rules

| ID | Rule |
|---|---|
| BR-01 | A restaurant can only be created with a valid, existing `ownerId`. |
| BR-02 | A restaurant can only be linked to an existing `addressId`. |
| BR-03 | Menu category names are unique **per restaurant** — the same name may exist in different restaurants. |
| BR-04 | Menu item names are unique **per category** — the same name may exist in different categories. |
| BR-05 | Deleting a restaurant is a hard delete and cascades through the entire category → item tree. |
| BR-06 | Deleting a category is a hard delete and removes all its items. |
| BR-07 | `rating` is an externally managed field (e.g., computed by an order/review service); the restaurant API accepts direct writes to it. |
| BR-08 | `isOpen` can be toggled freely via the update endpoint. |

---

## 6. Error Catalogue

All error responses follow the envelope with HTTP status code set accordingly.

| Error Code | HTTP | Constant | Message |
|---|---|---|---|
| `40030` | 400 | `RESTAURANT_NOT_FOUND` | The restaurant could not be found. |
| `40031` | 400 | `MENU_ITEM_NOT_FOUND` | The selected food item is no longer available. |
| `40032` | 400 | `RESTAURANT_CLOSED` | The restaurant is currently closed. |
| `40033` | 400 | `OUT_OF_STOCK` | One or more items in your order are out of stock. |
| `40034` | 400 | `MENU_CATEGORY_NOT_FOUND` | The menu category could not be found. |
| `40035` | 400 | `MENU_CATEGORY_ALREADY_EXISTS` | A menu category with this name already exists in this restaurant. |
| `40036` | 400 | `MENU_ITEM_ALREADY_EXISTS` | A menu item with this name already exists in this category. |
| `40053` | 400 | `ADDRESS_NOT_FOUND` | The address could not be found. |
| `40016` | 400 | `USER_NOT_FOUND` | The user you are looking for could not be found. |
| `40000` | 400 | `INVALID_INPUT` | Invalid input. Please check your data. (bean validation failures) |
| `40100` | 401 | `UNAUTHENTICATED` | You must be logged in to perform this action. |
| `40300` | 403 | `FORBIDDEN` | You do not have permission to access this resource. |

---

## 7. API Specification

### Conventions

- **Base URL:** `http://localhost:8080`
- **Auth:** `Authorization: Bearer <jwt>` header (where required)
- **Content-Type:** `application/json`
- **ID type:** UUID (e.g. `550e8400-e29b-41d4-a716-446655440000`)
- **Timestamp format:** ISO 8601 with timezone offset (e.g. `2026-07-07T04:30:00+07:00`)

---

### 7.1 Restaurant Endpoints

---

#### `POST /api/restaurants` — Create Restaurant

Creates a new restaurant.

**Request Body**

```json
{
  "ownerId": "uuid (required)",
  "name": "string (required, max 150)",
  "description": "string (optional)",
  "logoUrl": "string (optional)",
  "bannerUrl": "string (optional)",
  "isOpen": "boolean (optional, default: true)"
}
```

**Validation**

| Field | Rule |
|---|---|
| `ownerId` | Not null; must reference an existing user |
| `name` | Not blank; max 150 chars |

**Response `201 Created`**

```json
{
  "status": 201,
  "message": "Restaurant created successfully",
  "data": {
    "id": "uuid",
    "ownerId": "uuid",
    "name": "Phở Hà Nội",
    "addressId": null,
    "description": "Authentic Northern Vietnamese cuisine",
    "logoUrl": "https://cdn.example.com/logo.png",
    "bannerUrl": "https://cdn.example.com/banner.jpg",
    "rating": 0.00,
    "isOpen": true,
    "createdAt": "2026-07-07T04:30:00+07:00",
    "updatedAt": "2026-07-07T04:30:00+07:00"
  }
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| `ownerId` references non-existent user | `40016` | 400 |
| Bean validation fails | `40000` | 400 |

---

#### `GET /api/restaurants` — List Restaurants

Returns a paginated list of restaurants with optional RSQL filtering.

**Query Parameters**

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `filter` | string | No | — | RSQL filter expression (see §8) |
| `page` | int | No | `0` | Zero-based page index |
| `size` | int | No | `20` | Page size (server-bounded) |
| `sort` | string | No | `name,asc` | Sort field and direction |

**Common Filter Examples**

| Goal | RSQL Expression |
|---|---|
| Open restaurants | `filter=isOpen==true` |
| Rating ≥ 4.0 | `filter=rating=ge=4.0` |
| Name contains "pizza" | `filter=name=like=pizza` |
| Open AND rating ≥ 4.0 | `filter=isOpen==true;rating=ge=4.0` |
| Name or description contains "burger" | `filter=name=like=burger,description=like=burger` |

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Restaurants retrieved successfully",
  "data": {
    "content": [
      {
        "id": "uuid",
        "ownerId": "uuid",
        "name": "Phở Hà Nội",
        "addressId": "uuid",
        "description": "...",
        "logoUrl": "...",
        "bannerUrl": "...",
        "rating": 4.50,
        "isOpen": true,
        "createdAt": "2026-07-07T04:30:00+07:00",
        "updatedAt": "2026-07-07T04:30:00+07:00"
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

#### `GET /api/restaurants/{id}` — Get Restaurant Detail

Returns a single restaurant with its full menu tree (categories → items), ordered by `sortOrder` ascending.

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID | Restaurant identifier |

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Restaurant retrieved successfully",
  "data": {
    "id": "uuid",
    "ownerId": "uuid",
    "name": "Phở Hà Nội",
    "addressId": "uuid",
    "description": "...",
    "logoUrl": "...",
    "bannerUrl": "...",
    "rating": 4.50,
    "isOpen": true,
    "menu": [
      {
        "id": "uuid",
        "restaurantId": "uuid",
        "name": "Appetisers",
        "sortOrder": 1,
        "items": [
          {
            "id": "uuid",
            "categoryId": "uuid",
            "name": "Spring Rolls",
            "description": "Crispy fried spring rolls",
            "price": 35000.00,
            "imageUrl": "...",
            "isAvailable": true
          }
        ]
      }
    ]
  }
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Restaurant not found | `40030` | 400 |

---

#### `PUT /api/restaurants/{id}` — Update Restaurant

Partial update — any field may be omitted; only supplied non-null fields are changed.

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID | Restaurant identifier |

**Request Body**

```json
{
  "name": "string (optional, max 150)",
  "rating": "decimal (optional, max 5)",
  "addressId": "uuid (optional)",
  "description": "string (optional)",
  "logoUrl": "string (optional)",
  "bannerUrl": "string (optional)",
  "isOpen": "boolean (optional)"
}
```

**Validation**

| Field | Rule |
|---|---|
| `name` | If present, max 150 chars |
| `rating` | If present, max value 5 (size constraint) |
| `addressId` | If present, must reference an existing address |

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Restaurant updated successfully",
  "data": { /* RestaurantResponse */ }
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Restaurant not found | `40030` | 400 |
| `addressId` references non-existent address | `40053` | 400 |

---

#### `DELETE /api/restaurants/{id}` — Delete Restaurant

Hard-deletes the restaurant and cascades to all its categories and items.

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID | Restaurant identifier |

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Restaurant deleted successfully"
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Restaurant not found | `40030` | 400 |

---

### 7.2 Menu Category Endpoints

---

#### `POST /api/restaurants/{restaurantId}/categories` — Create Menu Category

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `restaurantId` | UUID | Parent restaurant identifier |

**Request Body**

```json
{
  "name": "string (required, max 100)",
  "sortOrder": "integer (optional, default: 0)"
}
```

**Validation**

| Field | Rule |
|---|---|
| `name` | Not blank; max 100 chars; unique within restaurant |

**Response `201 Created`**

```json
{
  "status": 201,
  "message": "Menu category created successfully",
  "data": {
    "id": "uuid",
    "restaurantId": "uuid",
    "name": "Appetisers",
    "sortOrder": 1,
    "items": []
  }
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Restaurant not found | `40030` | 400 |
| Category name already exists in restaurant | `40035` | 400 |

---

#### `GET /api/restaurants/{restaurantId}/categories` — List Categories (Paginated)

Returns a paginated list of categories for a restaurant.

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `restaurantId` | UUID | Parent restaurant identifier |

**Query Parameters**

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `keyword` | string | No | — | Partial name match |
| `page` | int | No | `0` | Page index |
| `size` | int | No | `20` | Page size |
| `sort` | string | No | — | Sort field and direction |

> [!NOTE]
> This endpoint still uses a dedicated `keyword` parameter (not RSQL). Each category in the response includes its embedded `items` list.

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Menu categories retrieved successfully",
  "data": {
    "content": [ /* MenuCategoryResponse[] */ ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 5,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Restaurant not found | `40030` | 400 |

---

#### `GET /api/categories/{id}` — Get Category by ID

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID | Category identifier |

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Menu category retrieved successfully",
  "data": {
    "id": "uuid",
    "restaurantId": "uuid",
    "name": "Main Course",
    "sortOrder": 2,
    "items": [ /* MenuItemResponse[] */ ]
  }
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Category not found | `40034` | 400 |

---

#### `PUT /api/categories/{id}` — Update Menu Category

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID | Category identifier |

**Request Body**

```json
{
  "name": "string (optional, max 100)",
  "sortOrder": "integer (optional)"
}
```

**Validation**

| Field | Rule |
|---|---|
| `name` | If present, max 100 chars; must be unique in the restaurant if changed |

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Menu category updated successfully",
  "data": { /* MenuCategoryResponse */ }
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Category not found | `40034` | 400 |
| New name already exists in the same restaurant | `40035` | 400 |

---

#### `DELETE /api/categories/{id}` — Delete Menu Category

Hard-deletes the category and all its items.

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Menu category deleted successfully"
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Category not found | `40034` | 400 |

---

### 7.3 Menu Item Endpoints

---

#### `POST /api/categories/{categoryId}/items` — Create Menu Item

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `categoryId` | UUID | Parent category identifier |

**Request Body**

```json
{
  "name": "string (required, max 150)",
  "description": "string (optional)",
  "price": "decimal (required, > 0)",
  "imageUrl": "string (optional)",
  "isAvailable": "boolean (optional)"
}
```

**Validation**

| Field | Rule |
|---|---|
| `name` | Not blank; max 150 chars; unique within category |
| `price` | Not null; must be > 0.0 |

**Response `201 Created`**

```json
{
  "status": 201,
  "message": "Menu item created successfully",
  "data": {
    "id": "uuid",
    "categoryId": "uuid",
    "name": "Phở Bò",
    "description": "Beef noodle soup with fresh herbs",
    "price": 65000.00,
    "imageUrl": "https://cdn.example.com/pho.jpg",
    "isAvailable": true
  }
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Category not found | `40034` | 400 |
| Item name already exists in category | `40036` | 400 |
| `price` ≤ 0 | `40000` | 400 |

---

#### `GET /api/categories/{categoryId}/items` — List Items (Paginated)

**Path Parameters**

| Parameter | Type | Description |
|---|---|---|
| `categoryId` | UUID | Parent category identifier |

**Query Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `keyword` | string | No | Partial match on name/description |
| `minPrice` | decimal | No | Lower bound (inclusive) for price |
| `maxPrice` | decimal | No | Upper bound (inclusive) for price |
| `isAvailable` | boolean | No | Filter by availability |
| `page` | int | No | Page index (default `0`) |
| `size` | int | No | Page size (default `20`) |
| `sort` | string | No | Sort field and direction |

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Menu items retrieved successfully",
  "data": {
    "content": [
      {
        "id": "uuid",
        "categoryId": "uuid",
        "name": "Phở Bò",
        "description": "...",
        "price": 65000.00,
        "imageUrl": "...",
        "isAvailable": true
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

#### `GET /api/items/{id}` — Get Item by ID

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Menu item retrieved successfully",
  "data": {
    "id": "uuid",
    "categoryId": "uuid",
    "name": "Phở Bò",
    "description": "...",
    "price": 65000.00,
    "imageUrl": "...",
    "isAvailable": true
  }
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Item not found | `40031` | 400 |

---

#### `PUT /api/items/{id}` — Update Menu Item

**Request Body**

```json
{
  "name": "string (optional, max 150)",
  "description": "string (optional)",
  "price": "decimal (optional, > 0)",
  "imageUrl": "string (optional)",
  "isAvailable": "boolean (optional)"
}
```

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Menu item updated successfully",
  "data": { /* MenuItemResponse */ }
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Item not found | `40031` | 400 |
| `price` ≤ 0 | `40000` | 400 |

---

#### `DELETE /api/items/{id}` — Delete Menu Item

**Response `200 OK`**

```json
{
  "status": 200,
  "message": "Menu item deleted successfully"
}
```

**Error Responses**

| Condition | Error Code | HTTP |
|---|---|---|
| Item not found | `40031` | 400 |

---

## 8. Filtering Reference (RSQL)

The `GET /api/restaurants` endpoint accepts an RSQL `filter` parameter powered by [rsql-jpa-specification](https://github.com/perplexhub/rsql-jpa-specification).

### 8.1 Supported Operators

| Operator | Meaning | Example |
|---|---|---|
| `==` | Equal | `isOpen==true` |
| `=like=` | LIKE `%value%` | `name=like=pho` |
| `=ilike=` | Case-insensitive LIKE | `name=ilike=PHO` |
| `=gt=` / `>` | Greater than | `rating=gt=3.0` |
| `=ge=` / `>=` | Greater than or equal | `rating=ge=4.0` |
| `=lt=` / `<` | Less than | `rating=lt=5.0` |
| `=le=` / `<=` | Less than or equal | `rating=le=4.5` |
| `=in=` | In list | `rating=in=(4.0,4.5,5.0)` |
| `=out=` | Not in list | `isOpen=out=(false)` |
| `=isnull=` | Is null | `addressId=isnull=` |
| `=isnotnull=` | Is not null | `addressId=isnotnull=` |
| `=bt=` | Between (inclusive) | `rating=bt=(3.5,5.0)` |

### 8.2 Logical Connectors

| Connector | Symbol | Meaning |
|---|---|---|
| AND | `;` | All conditions must match |
| OR | `,` | Any condition must match |

### 8.3 Filterable Fields

The following `Restaurant` entity fields can be used in filter expressions:

| RSQL Field | Java Field | Type |
|---|---|---|
| `name` | `name` | String |
| `description` | `description` | String |
| `rating` | `rating` | BigDecimal |
| `isOpen` | `isOpen` | Boolean |
| `createdAt` | `createdAt` | OffsetDateTime |
| `updatedAt` | `updatedAt` | OffsetDateTime |

### 8.4 Filter Examples

```
# Open restaurants with rating ≥ 4.0
GET /api/restaurants?filter=isOpen==true;rating=ge=4.0

# Restaurants whose name contains "burger" (case-insensitive)
GET /api/restaurants?filter=name=ilike=burger

# Restaurants created after 2026-01-01
GET /api/restaurants?filter=createdAt=gt=2026-01-01T00:00:00%2B07:00

# Combine with sorting and pagination
GET /api/restaurants?filter=isOpen==true&sort=rating,desc&page=0&size=10
```

> [!WARNING]
> The `filter` parameter value must be URL-encoded when it contains special characters such as `+`, `=`, `>`, `<`, `(`, `)`.

---

## 9. Non-Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Performance | List endpoints **should** respond within 300 ms for page sizes ≤ 50 under normal load. |
| NFR-02 | Pagination | All list endpoints **must** enforce a server-side page-size cap to prevent unbounded queries. |
| NFR-03 | Transactions | All write operations run inside a `@Transactional` boundary; reads use `@Transactional(readOnly = true)`. |
| NFR-04 | Cascade Safety | Deletion cascades are handled programmatically in the service layer, not via JPA `CascadeType.REMOVE`, to ensure explicit control. |
| NFR-05 | Uniqueness | Category name uniqueness per restaurant and item name uniqueness per category are enforced at the application layer before DB insert. |
| NFR-06 | Observability | Spring Boot Actuator endpoints are enabled for health and metrics. |
| NFR-07 | API Documentation | Swagger/OpenAPI UI is available at `/swagger-ui.html` via `springdoc-openapi`. |
