

-- V5: Add PostGIS extension and geospatial search support for restaurants
-- Reference: https://neon.com/guides/geospatial-search

-- 1. Install and enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. Add geometry column (SRID 4326 representing WGS 84 latitude/longitude)
ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS geom GEOMETRY(Point, 4326);

-- 3. Populate geom for existing address rows with valid lat and lng
UPDATE addresses
SET geom = ST_SetSRID(ST_MakePoint(lng, lat), 4326)
WHERE lat IS NOT NULL AND lng IS NOT NULL;

-- 4. Automatically keep geom synchronized whenever lat or lng is inserted or updated
CREATE OR REPLACE FUNCTION update_address_geom()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.lat IS NOT NULL AND NEW.lng IS NOT NULL THEN
        NEW.geom := ST_SetSRID(ST_MakePoint(NEW.lng, NEW.lat), 4326);
    ELSE
        NEW.geom := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_address_geom ON addresses;

CREATE TRIGGER trigger_update_address_geom
BEFORE INSERT OR UPDATE OF lat, lng ON addresses
FOR EACH ROW
EXECUTE FUNCTION update_address_geom();

-- 5. Create GiST index for fast geospatial lookups
CREATE INDEX IF NOT EXISTS addresses_geom_gist ON addresses USING GIST(geom);
