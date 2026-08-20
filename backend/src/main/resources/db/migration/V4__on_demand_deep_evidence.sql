CREATE TABLE restaurant_deep_evidence (
    id                  UUID PRIMARY KEY,
    restaurant_id       UUID NOT NULL REFERENCES restaurant (id) ON DELETE CASCADE,
    source              VARCHAR(24) NOT NULL,
    status              VARCHAR(16) NOT NULL,
    query_fingerprint   VARCHAR(64) NOT NULL,
    evidence_json       JSONB NOT NULL DEFAULT '[]'::jsonb,
    fetched_at          TIMESTAMP NOT NULL,
    expires_at          TIMESTAMP NOT NULL,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    CONSTRAINT uq_restaurant_deep_evidence UNIQUE (restaurant_id, source)
);

CREATE INDEX idx_restaurant_deep_evidence_expiry
    ON restaurant_deep_evidence (restaurant_id, expires_at);

CREATE TABLE restaurant_deep_analysis (
    id                    UUID PRIMARY KEY,
    restaurant_id         UUID NOT NULL REFERENCES restaurant (id) ON DELETE CASCADE,
    evidence_fingerprint  VARCHAR(64) NOT NULL,
    analysis_json         JSONB NOT NULL,
    algorithm_version     VARCHAR(32) NOT NULL,
    generated_at          TIMESTAMP NOT NULL,
    expires_at            TIMESTAMP NOT NULL,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP NOT NULL,
    CONSTRAINT uq_restaurant_deep_analysis UNIQUE (restaurant_id)
);

CREATE INDEX idx_restaurant_deep_analysis_expiry
    ON restaurant_deep_analysis (restaurant_id, expires_at);
