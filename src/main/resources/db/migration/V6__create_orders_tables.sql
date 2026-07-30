-- V6: Create orders, order_items, order_pricings, and payments tables
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE orders (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES users(id),
    restaurant_id        UUID NOT NULL REFERENCES restaurants(id),
    delivery_address_id  UUID REFERENCES addresses(id),
    delivery_method      VARCHAR(20) NOT NULL,
    status               VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    special_instructions TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE order_pricings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    subtotal        DECIMAL(10, 2) NOT NULL,
    delivery_fee    DECIMAL(10, 2) DEFAULT 0.00,
    platform_fee    DECIMAL(10, 2) DEFAULT 0.00,
    discount_amount DECIMAL(10, 2) DEFAULT 0.00,
    total_amount    DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_order_pricings_order_id UNIQUE(order_id)
);

CREATE TABLE order_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_items(id),
    quantity     INT NOT NULL,
    unit_price   DECIMAL(10, 2) NOT NULL,
    special_notes TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    amount        DECIMAL(10, 2) NOT NULL,
    currency      VARCHAR(3) NOT NULL DEFAULT 'VND',
    method_type   VARCHAR(30),
    gateway_token VARCHAR(255),
    party_name    VARCHAR(50),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_payments_order_id UNIQUE(order_id)
);

-- Indexing foreign keys for performance
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_restaurant_id ON orders(restaurant_id);
CREATE INDEX idx_orders_delivery_address_id ON orders(delivery_address_id);
CREATE INDEX idx_order_pricings_order_id ON order_pricings(order_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_menu_item_id ON order_items(menu_item_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);
