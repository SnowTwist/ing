
CREATE TABLE orders (
    id          uuid        PRIMARY KEY,
    customer_id varchar(64) NOT NULL,
    status      varchar(32) NOT NULL,
    created_at  timestamptz NOT NULL
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);

CREATE TABLE outbox_events (
    id              uuid        PRIMARY KEY,
    aggregate_type  varchar(64) NOT NULL,
    aggregate_id    varchar(64) NOT NULL,
    event_type      varchar(64) NOT NULL,
    payload         jsonb       NOT NULL,
    status          varchar(16) NOT NULL,
    attempts        integer     NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL,
    created_at      timestamptz NOT NULL,
    published_at    timestamptz,
    last_error      text,
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_due
    ON outbox_events (next_attempt_at, created_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_published_at
    ON outbox_events (published_at)
    WHERE status = 'PUBLISHED';

COMMENT ON TABLE outbox_events IS
    'Transactional outbox. Rows are written in the same transaction as the aggregate change and '
    'relayed asynchronously with at-least-once delivery.';
