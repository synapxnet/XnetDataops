-- 创建不登录的最小权限角色；登录账户与密码由部署系统单独创建和注入。
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'xnet_dataops_product_writer') THEN
        CREATE ROLE xnet_dataops_product_writer NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'xnet_mlops_product_reader') THEN
        CREATE ROLE xnet_mlops_product_reader NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA recommendation_raw TO xnet_dataops_product_writer;
GRANT USAGE ON SCHEMA recommendation_curated TO xnet_dataops_product_writer;
GRANT SELECT ON ALL TABLES IN SCHEMA recommendation_raw TO xnet_dataops_product_writer;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA recommendation_curated
    TO xnet_dataops_product_writer;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA recommendation_curated
    TO xnet_dataops_product_writer;

GRANT USAGE ON SCHEMA recommendation_curated TO xnet_mlops_product_reader;
GRANT SELECT ON recommendation_curated.dcn_training TO xnet_mlops_product_reader;
GRANT SELECT ON recommendation_curated.data_product_registry TO xnet_mlops_product_reader;

ALTER DEFAULT PRIVILEGES IN SCHEMA recommendation_raw
    GRANT SELECT ON TABLES TO xnet_dataops_product_writer;
ALTER DEFAULT PRIVILEGES IN SCHEMA recommendation_curated
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO xnet_dataops_product_writer;
