CREATE TABLE subscriptions (
    subscriber_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (subscriber_id, author_id),
    CHECK (subscriber_id <> author_id)
);

CREATE TABLE subscription_sets (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    author_ids BIGINT[] NOT NULL UNIQUE
);

CREATE TABLE user_subscription (
    user_id BIGINT PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription_sets (id)
);

CREATE INDEX idx_subscriptions_author ON subscriptions (author_id);
