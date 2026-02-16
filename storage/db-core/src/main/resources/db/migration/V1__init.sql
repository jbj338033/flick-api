CREATE TABLE users
(
    id               UUID PRIMARY KEY,
    dauth_id         VARCHAR(255) NOT NULL UNIQUE,
    name             VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    role             VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    balance          INT          NOT NULL DEFAULT 0,
    grade            INT,
    room             INT,
    number           INT,
    is_grant_claimed BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL
);

CREATE TABLE booths
(
    id           UUID PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    pairing_code VARCHAR(4),
    user_id      UUID         NOT NULL UNIQUE REFERENCES users (id),
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE TABLE products
(
    id             UUID PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    price          INT          NOT NULL,
    stock          INT,
    is_sold_out    BOOLEAN      NOT NULL DEFAULT FALSE,
    purchase_limit INT,
    booth_id       UUID         NOT NULL REFERENCES booths (id),
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL
);

CREATE TABLE orders
(
    id           UUID PRIMARY KEY,
    order_number INT         NOT NULL,
    total_amount INT         NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    user_id      UUID        REFERENCES users (id),
    booth_id     UUID        NOT NULL REFERENCES booths (id),
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL
);

CREATE TABLE order_items
(
    id         UUID PRIMARY KEY,
    quantity   INT       NOT NULL,
    price      INT       NOT NULL,
    order_id   UUID      NOT NULL REFERENCES orders (id),
    product_id UUID      NOT NULL REFERENCES products (id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE order_item_options
(
    id            UUID PRIMARY KEY,
    group_name    VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    price         INT          NOT NULL,
    quantity      INT          NOT NULL DEFAULT 1,
    order_item_id UUID         NOT NULL REFERENCES order_items (id),
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);

CREATE TABLE payment_requests
(
    id           UUID PRIMARY KEY,
    code         VARCHAR(6) NOT NULL UNIQUE,
    order_id     UUID       NOT NULL UNIQUE REFERENCES orders (id),
    expires_at   TIMESTAMP  NOT NULL,
    is_confirmed BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP  NOT NULL,
    updated_at   TIMESTAMP  NOT NULL
);

CREATE TABLE transactions
(
    id             UUID PRIMARY KEY,
    type           VARCHAR(20) NOT NULL,
    amount         INT         NOT NULL,
    balance_before INT         NOT NULL,
    balance_after  INT         NOT NULL,
    user_id        UUID        NOT NULL REFERENCES users (id),
    order_id       UUID,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL
);

CREATE TABLE notifications
(
    id         UUID PRIMARY KEY,
    type       VARCHAR(30)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    user_id    UUID         NOT NULL REFERENCES users (id),
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE TABLE notices
(
    id         UUID PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    user_id    UUID         NOT NULL REFERENCES users (id),
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE TABLE refund_requests
(
    id             UUID PRIMARY KEY,
    bank           VARCHAR(50) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    amount         INT         NOT NULL,
    user_id        UUID        NOT NULL UNIQUE REFERENCES users (id),
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL
);

CREATE TABLE face_embeddings
(
    id         UUID PRIMARY KEY,
    user_id    UUID      NOT NULL REFERENCES users (id),
    embedding  BYTEA     NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE product_option_groups
(
    id             UUID PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    is_required    BOOLEAN      NOT NULL DEFAULT FALSE,
    max_selections INT          NOT NULL DEFAULT 1,
    sort_order     INT          NOT NULL DEFAULT 0,
    product_id     UUID         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL
);

CREATE TABLE product_options
(
    id                    UUID PRIMARY KEY,
    name                  VARCHAR(255) NOT NULL,
    price                 INT          NOT NULL,
    is_quantity_selectable BOOLEAN     NOT NULL DEFAULT FALSE,
    sort_order            INT          NOT NULL DEFAULT 0,
    option_group_id       UUID         NOT NULL REFERENCES product_option_groups (id) ON DELETE CASCADE,
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL
);

CREATE INDEX idx_users_grade_room_number ON users (grade, room, number);
CREATE UNIQUE INDEX idx_booths_pairing_code ON booths (pairing_code);
CREATE INDEX idx_products_booth_id ON products (booth_id);
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_booth_id ON orders (booth_id);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
CREATE INDEX idx_order_item_options_order_item_id ON order_item_options (order_item_id);
CREATE INDEX idx_payment_requests_unconfirmed ON payment_requests (expires_at) WHERE is_confirmed = FALSE;
CREATE INDEX idx_transactions_user_id_created_at ON transactions (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_id_is_read ON notifications (user_id, is_read);
CREATE INDEX idx_face_embeddings_user_id ON face_embeddings (user_id);
CREATE INDEX idx_product_option_groups_product_id ON product_option_groups (product_id, sort_order);
CREATE INDEX idx_product_options_option_group_id ON product_options (option_group_id, sort_order);
