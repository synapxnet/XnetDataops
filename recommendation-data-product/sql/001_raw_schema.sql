-- 真实推荐数据的 PostgreSQL 原始层、数据产品层和版本登记表。
CREATE SCHEMA IF NOT EXISTS recommendation_raw;
CREATE SCHEMA IF NOT EXISTS recommendation_curated;

CREATE TABLE IF NOT EXISTS recommendation_raw.users (
    user_key VARCHAR(32) PRIMARY KEY,
    status VARCHAR(64) NOT NULL,
    user_type VARCHAR(64) NOT NULL,
    sex VARCHAR(64) NOT NULL,
    manufacturer_type VARCHAR(64) NOT NULL,
    source VARCHAR(64) NOT NULL,
    real_status VARCHAR(64) NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recommendation_raw.items (
    item_key VARCHAR(32) PRIMARY KEY,
    item_type VARCHAR(64) NOT NULL,
    category_key VARCHAR(32) NOT NULL,
    tag_set_key VARCHAR(32) NOT NULL,
    status VARCHAR(64) NOT NULL,
    publish_status VARCHAR(64) NOT NULL,
    duration_seconds DOUBLE PRECISION NOT NULL,
    publish_type VARCHAR(64) NOT NULL,
    manufacturer_type VARCHAR(64) NOT NULL,
    article_format VARCHAR(64) NOT NULL,
    video_type VARCHAR(64) NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recommendation_raw.interactions (
    event_key VARCHAR(32) PRIMARY KEY,
    user_key VARCHAR(32) NOT NULL REFERENCES recommendation_raw.users(user_key),
    item_key VARCHAR(32) NOT NULL REFERENCES recommendation_raw.items(item_key),
    behavior_type VARCHAR(64) NOT NULL,
    duration_ms BIGINT NOT NULL,
    read_percent DOUBLE PRECISION NOT NULL,
    like_status INTEGER NOT NULL,
    event_date VARCHAR(32) NOT NULL,
    label SMALLINT NOT NULL CHECK (label IN (0, 1)),
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recommendation_interactions_user
    ON recommendation_raw.interactions(user_key);
CREATE INDEX IF NOT EXISTS idx_recommendation_interactions_item
    ON recommendation_raw.interactions(item_key);
CREATE INDEX IF NOT EXISTS idx_recommendation_interactions_label
    ON recommendation_raw.interactions(label);

CREATE TABLE IF NOT EXISTS recommendation_curated.dcn_training (
    product_version VARCHAR(128) NOT NULL,
    event_key VARCHAR(32) NOT NULL,
    user_key VARCHAR(32) NOT NULL,
    item_key VARCHAR(32) NOT NULL,
    behavior_type VARCHAR(64) NOT NULL,
    user_type VARCHAR(64) NOT NULL,
    user_sex VARCHAR(64) NOT NULL,
    user_manufacturer_type VARCHAR(64) NOT NULL,
    user_source VARCHAR(64) NOT NULL,
    item_type VARCHAR(64) NOT NULL,
    item_category_key VARCHAR(32) NOT NULL,
    item_tag_set_key VARCHAR(32) NOT NULL,
    item_duration_seconds DOUBLE PRECISION NOT NULL,
    behavior_duration_ms BIGINT NOT NULL,
    read_percent DOUBLE PRECISION NOT NULL,
    like_status INTEGER NOT NULL,
    label SMALLINT NOT NULL CHECK (label IN (0, 1)),
    event_date VARCHAR(32) NOT NULL,
    dataset_split VARCHAR(16) NOT NULL CHECK (dataset_split IN ('train', 'validation', 'test')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_version, event_key)
);

CREATE TABLE IF NOT EXISTS recommendation_curated.data_product_registry (
    product_name VARCHAR(128) NOT NULL,
    product_version VARCHAR(128) PRIMARY KEY,
    row_count BIGINT NOT NULL,
    positive_count BIGINT NOT NULL,
    negative_count BIGINT NOT NULL,
    schema_digest_sha256 CHAR(64) NOT NULL,
    artifact_digest_sha256 CHAR(64) NOT NULL,
    lineage_reference VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('building', 'validated', 'published', 'retired')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS recommendation_curated.data_product_execution (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    action VARCHAR(32) NOT NULL CHECK (action IN ('build', 'publish')),
    product_version VARCHAR(128) NOT NULL,
    approval_id VARCHAR(128) NOT NULL,
    request_digest_sha256 CHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('running', 'succeeded', 'failed')),
    result_summary VARCHAR(512),
    error_summary VARCHAR(512),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMPTZ
);

COMMENT ON SCHEMA recommendation_raw IS 'XnetDataOps 管理的脱敏推荐原始数据层';
COMMENT ON SCHEMA recommendation_curated IS 'XnetDataOps 发布给 XnetMLOps 的版本化训练数据产品层';
COMMENT ON TABLE recommendation_curated.data_product_registry IS '训练数据产品的版本、契约、血缘和发布状态登记表';
COMMENT ON TABLE recommendation_curated.data_product_execution IS '数据产品构建和发布动作的审批、幂等与审计回执';
