-- ELMA 家今天的饭 V0.1 初始表结构(六张核心表,主键全部 UUID)

CREATE TABLE restaurant (
    id                UUID PRIMARY KEY,
    source            VARCHAR(16)  NOT NULL,
    source_poi_id     VARCHAR(64)  NOT NULL,
    name              VARCHAR(120) NOT NULL,
    latitude          DOUBLE PRECISION NOT NULL,
    longitude         DOUBLE PRECISION NOT NULL,
    category_code     VARCHAR(32)  NOT NULL,
    category_label    VARCHAR(30)  NOT NULL,
    rating            DOUBLE PRECISION,
    review_count      INTEGER,
    average_price     INTEGER,
    business_status   VARCHAR(16)  NOT NULL,
    opening_hours     VARCHAR(255),
    address           VARCHAR(255),
    data_completeness VARCHAR(16)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    CONSTRAINT uq_restaurant_source_poi UNIQUE (source, source_poi_id)
);

CREATE TABLE risk_result (
    id                UUID PRIMARY KEY,
    restaurant_id     UUID NOT NULL REFERENCES restaurant (id),
    risk_score        INTEGER NOT NULL,
    risk_level        VARCHAR(16) NOT NULL,
    reasons_json      JSONB   NOT NULL,
    algorithm_version VARCHAR(32) NOT NULL,
    calculated_at     TIMESTAMP NOT NULL
);
CREATE INDEX idx_risk_result_restaurant ON risk_result (restaurant_id, calculated_at);

CREATE TABLE recommendation_log (
    id                             UUID PRIMARY KEY,
    anonymous_user_id              UUID NOT NULL,
    request_condition_json         JSONB NOT NULL,
    candidate_count                INTEGER NOT NULL,
    current_restaurant_id          UUID NOT NULL REFERENCES restaurant (id),
    risk_score                     INTEGER NOT NULL,
    low_regret_score               DOUBLE PRECISION NOT NULL,
    risk_algorithm_version         VARCHAR(32) NOT NULL,
    recommendation_algorithm_version VARCHAR(32) NOT NULL,
    created_at                     TIMESTAMP NOT NULL
);
CREATE INDEX idx_recommendation_log_user ON recommendation_log (anonymous_user_id, created_at);

CREATE TABLE recommendation_candidate (
    id                      UUID PRIMARY KEY,
    recommendation_log_id   UUID NOT NULL REFERENCES recommendation_log (id) ON DELETE CASCADE,
    restaurant_id           UUID NOT NULL REFERENCES restaurant (id),
    slot                    INTEGER NOT NULL,
    distance_meters         INTEGER NOT NULL,
    risk_score              INTEGER NOT NULL,
    risk_level              VARCHAR(16) NOT NULL,
    risk_reasons_json       JSONB NOT NULL,
    risk_algorithm_version  VARCHAR(32) NOT NULL,
    low_regret_score        DOUBLE PRECISION NOT NULL,
    reasons_json            JSONB NOT NULL,
    shown                   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_candidate_log_slot UNIQUE (recommendation_log_id, slot)
);
CREATE INDEX idx_recommendation_candidate_log ON recommendation_candidate (recommendation_log_id);

CREATE TABLE user_feedback (
    id                   UUID PRIMARY KEY,
    recommendation_log_id UUID NOT NULL REFERENCES recommendation_log (id),
    restaurant_id        UUID NOT NULL REFERENCES restaurant (id),
    anonymous_user_id    UUID NOT NULL,
    result               VARCHAR(16) NOT NULL,
    created_at           TIMESTAMP NOT NULL
);
CREATE INDEX idx_user_feedback_log ON user_feedback (recommendation_log_id);

CREATE TABLE user_preference (
    id               UUID PRIMARY KEY,
    anonymous_user_id UUID NOT NULL,
    preference_json  JSONB NOT NULL,
    created_at       TIMESTAMP NOT NULL
);
CREATE INDEX idx_user_preference_user ON user_preference (anonymous_user_id, created_at);
