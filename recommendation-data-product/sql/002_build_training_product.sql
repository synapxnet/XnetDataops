-- 由 DataOps 聚合任务从原始层重建 DCN 训练数据产品。
-- 调用前需执行：SET synapxnet.product_version = 'recommendation-dcn-demo-v1';
BEGIN;

DELETE FROM recommendation_curated.dcn_training
WHERE product_version = current_setting('synapxnet.product_version');

INSERT INTO recommendation_curated.dcn_training (
    product_version,
    event_key,
    user_key,
    item_key,
    behavior_type,
    user_type,
    user_sex,
    user_manufacturer_type,
    user_source,
    item_type,
    item_category_key,
    item_tag_set_key,
    item_duration_seconds,
    behavior_duration_ms,
    read_percent,
    like_status,
    label,
    event_date,
    dataset_split
)
SELECT
    current_setting('synapxnet.product_version'),
    interaction.event_key,
    interaction.user_key,
    interaction.item_key,
    interaction.behavior_type,
    user_profile.user_type,
    user_profile.sex,
    user_profile.manufacturer_type,
    user_profile.source,
    item.item_type,
    item.category_key,
    item.tag_set_key,
    item.duration_seconds,
    interaction.duration_ms,
    interaction.read_percent,
    interaction.like_status,
    interaction.label,
    interaction.event_date,
    CASE
        WHEN get_byte(decode(md5(interaction.event_key), 'hex'), 0) < 205 THEN 'train'
        WHEN get_byte(decode(md5(interaction.event_key), 'hex'), 0) < 230 THEN 'validation'
        ELSE 'test'
    END
FROM recommendation_raw.interactions AS interaction
INNER JOIN recommendation_raw.users AS user_profile
    ON user_profile.user_key = interaction.user_key
INNER JOIN recommendation_raw.items AS item
    ON item.item_key = interaction.item_key;

COMMIT;
