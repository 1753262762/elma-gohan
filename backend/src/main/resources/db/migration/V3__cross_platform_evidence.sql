ALTER TABLE restaurant
    ADD COLUMN telephone VARCHAR(128);

CREATE TABLE external_entity_mapping (
    id                    UUID PRIMARY KEY,
    primary_source        VARCHAR(16) NOT NULL,
    primary_poi_id        VARCHAR(64) NOT NULL,
    evidence_source       VARCHAR(16) NOT NULL,
    evidence_poi_id       VARCHAR(128),
    match_status          VARCHAR(16) NOT NULL,
    match_confidence      DOUBLE PRECISION,
    match_features_json   JSONB NOT NULL DEFAULT '{}'::jsonb,
    v3_evidence_json      JSONB,
    v2_evidence_json      JSONB,
    evidence_observed_at  TIMESTAMP,
    v2_observed_at        TIMESTAMP,
    expires_at            TIMESTAMP NOT NULL,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP NOT NULL,
    CONSTRAINT uq_external_entity_mapping
        UNIQUE (primary_source, primary_poi_id, evidence_source)
);
CREATE INDEX idx_external_entity_mapping_lookup
    ON external_entity_mapping (primary_source, primary_poi_id, evidence_source, expires_at);

ALTER TABLE risk_result
    ADD COLUMN evidence_summary_json JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE recommendation_candidate
    ADD COLUMN evidence_summary_json JSONB NOT NULL DEFAULT '{}'::jsonb;
