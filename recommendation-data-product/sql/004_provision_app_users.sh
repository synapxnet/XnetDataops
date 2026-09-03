#!/bin/sh
set -eu

# 使用容器环境中的秘密创建平台登录账户，并授予最小权限组角色。
psql \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=dataops_password="$RECOMMENDATION_DATAOPS_DB_PASSWORD" \
  --set=mlops_password="$RECOMMENDATION_MLOPS_DB_PASSWORD" <<'SQL'
CREATE ROLE recommendation_dataops LOGIN PASSWORD :'dataops_password';
GRANT xnet_dataops_product_writer TO recommendation_dataops;

CREATE ROLE recommendation_mlops_reader LOGIN PASSWORD :'mlops_password';
GRANT xnet_mlops_product_reader TO recommendation_mlops_reader;
SQL
