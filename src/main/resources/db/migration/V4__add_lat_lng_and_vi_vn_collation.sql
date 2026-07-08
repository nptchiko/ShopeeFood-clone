-- V4: Add lat/lng to addresses + apply Vietnamese ICU collation to text columns
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. ICU-based Vietnamese collation (requires PostgreSQL 12+ with ICU support)
--    Used for correct Vietnamese alphabetical sort order (à á â ã ạ…)
CREATE COLLATION IF NOT EXISTS vi_vn (provider = icu, locale = 'vi-VN');

-- 2. Add geographic coordinates to addresses
ALTER TABLE addresses
    ADD COLUMN latitude  DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;

-- 3. Apply vi_vn collation to user-visible text columns
--    This ensures ORDER BY / LIKE / index comparisons respect Vietnamese diacritics

-- users
ALTER TABLE users
    ALTER COLUMN name TYPE VARCHAR(100) COLLATE vi_vn;

-- restaurants
ALTER TABLE restaurants
    ALTER COLUMN name        TYPE VARCHAR(150) COLLATE vi_vn,
    ALTER COLUMN description TYPE TEXT         COLLATE vi_vn;

-- menu_categories
ALTER TABLE menu_categories
    ALTER COLUMN name TYPE VARCHAR(100) COLLATE vi_vn;

-- menu_items
ALTER TABLE menu_items
    ALTER COLUMN name        TYPE VARCHAR(150) COLLATE vi_vn,
    ALTER COLUMN description TYPE TEXT         COLLATE vi_vn;

-- addresses
ALTER TABLE addresses
    ALTER COLUMN label TYPE VARCHAR(50)  COLLATE vi_vn,
    ALTER COLUMN line1 TYPE VARCHAR(255) COLLATE vi_vn,
    ALTER COLUMN line2 TYPE VARCHAR(255) COLLATE vi_vn,
    ALTER COLUMN city  TYPE VARCHAR(100) COLLATE vi_vn;
