ALTER TABLE risk_result
    ADD COLUMN confidence DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    ADD COLUMN factors_json JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE recommendation_candidate
    ADD COLUMN risk_confidence DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    ADD COLUMN risk_factors_json JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE recommendation_log
    ADD COLUMN recommended_restaurant_id UUID REFERENCES restaurant (id);

UPDATE recommendation_log
SET recommended_restaurant_id = current_restaurant_id
WHERE recommended_restaurant_id IS NULL;

ALTER TABLE recommendation_log
    ALTER COLUMN recommended_restaurant_id SET NOT NULL;
