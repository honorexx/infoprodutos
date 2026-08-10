-- Preço por curso + pacotes + pedidos (Mercado Pago).

ALTER TABLE course
    ADD COLUMN IF NOT EXISTS price_cents BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'BRL';

CREATE TABLE IF NOT EXISTS product_package (
    id              UUID PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    slug            VARCHAR(220) NOT NULL,
    description     TEXT,
    price_cents     BIGINT NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'BRL',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_product_package_slug_active
    ON product_package (slug)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS product_package_course (
    package_id  UUID NOT NULL REFERENCES product_package (id),
    course_id   UUID NOT NULL REFERENCES course (id),
    PRIMARY KEY (package_id, course_id)
);

CREATE TABLE IF NOT EXISTS commerce_order (
    id                  UUID PRIMARY KEY,
    buyer_user_id       UUID NOT NULL REFERENCES app_user (id),
    kind                VARCHAR(20) NOT NULL,
    course_id           UUID REFERENCES course (id),
    package_id          UUID REFERENCES product_package (id),
    amount_cents        BIGINT NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'BRL',
    status              VARCHAR(20) NOT NULL,
    mp_preference_id    VARCHAR(120),
    mp_payment_id       VARCHAR(120),
    idempotency_key     VARCHAR(120) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_commerce_order_kind CHECK (kind IN ('COURSE', 'PACKAGE')),
    CONSTRAINT chk_commerce_order_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'REFUNDED')
    ),
    CONSTRAINT chk_commerce_order_target CHECK (
        (kind = 'COURSE' AND course_id IS NOT NULL AND package_id IS NULL)
        OR (kind = 'PACKAGE' AND package_id IS NOT NULL AND course_id IS NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_commerce_order_idempotency
    ON commerce_order (idempotency_key);

CREATE UNIQUE INDEX IF NOT EXISTS uq_commerce_order_mp_payment
    ON commerce_order (mp_payment_id)
    WHERE mp_payment_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_commerce_order_buyer
    ON commerce_order (buyer_user_id);

CREATE TABLE IF NOT EXISTS commerce_order_item (
    id          UUID PRIMARY KEY,
    order_id    UUID NOT NULL REFERENCES commerce_order (id) ON DELETE CASCADE,
    course_id   UUID NOT NULL REFERENCES course (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_commerce_order_item_order
    ON commerce_order_item (order_id);
