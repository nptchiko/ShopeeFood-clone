CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name          VARCHAR(100) NOT NULL,
  email         VARCHAR(255) UNIQUE NOT NULL,
  phone         VARCHAR(20),
  password_hash TEXT NOT NULL,
  role          VARCHAR(20) NOT NULL DEFAULT 'customer', -- customer | restaurant_owner | agent | admin
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE addresses (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
  label       VARCHAR(50),              -- e.g. "Home", "Work"
  line1       VARCHAR(255) NOT NULL,
  line2       VARCHAR(255),
  city        VARCHAR(100),
  postal_code VARCHAR(20),
  is_default  BOOLEAN DEFAULT false,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE restaurants (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id  UUID REFERENCES users(id),
  name           VARCHAR(150) NOT NULL,
  address_id     UUID REFERENCES addresses(id),
  description    TEXT,
  logo_url       TEXT,
  banner_url     TEXT,
  rating         DECIMAL(3,2) DEFAULT 0.0,
  is_open        BOOLEAN DEFAULT true,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE menu_categories (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
  name          VARCHAR(100) NOT NULL,
  sort_order    INT DEFAULT 0,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE menu_items (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  category_id   UUID NOT NULL REFERENCES menu_categories(id) ON DELETE CASCADE,
  name          VARCHAR(150) NOT NULL,
  description   TEXT,
  price         DECIMAL(10, 2) NOT NULL,
  image_url     TEXT,
  is_available  BOOLEAN DEFAULT true,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
