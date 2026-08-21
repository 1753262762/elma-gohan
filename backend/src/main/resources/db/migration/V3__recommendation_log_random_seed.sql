-- recommendation-v0.3:候选池加权随机种子落库,支持确定性重放(审计 P1-1)
ALTER TABLE recommendation_log
    ADD COLUMN random_seed BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN selection_snapshot_json JSONB NOT NULL DEFAULT '[]'::jsonb;
