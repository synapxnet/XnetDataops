# 推荐数据产品接入

该目录把真实 PostgreSQL 推荐业务数据转换为可由 XnetDataOps 管理、由 XnetMLOps 消费的版本化训练数据产品。原始 5.4 GB 数据和脱敏后的 CSV 均不进入 Git。

## 数据边界

- `recommendation_raw.users`：脱敏用户特征。
- `recommendation_raw.items`：脱敏内容特征。
- `recommendation_raw.interactions`：脱敏行为与监督标签。
- `recommendation_curated.dcn_training`：DataOps 聚合后发布给 MLOps 的训练产品。
- `recommendation_curated.data_product_registry`：版本、行数、摘要、血缘和发布状态。

姓名、昵称、IP、UDID、图片地址、学校、城市、正文和标题不会写入演示数据。用户、内容和事件标识使用带密钥 HMAC-SHA256 一致性脱敏，盐值不得提交到仓库。

## 构建 8 万条演示数据

在 PowerShell 中设置一个长期保存于密码管理器的随机盐值，同一版本重复构建必须使用相同值：

```powershell
$env:RECOMMENDATION_SAMPLE_SALT = '<至少 16 个字符的秘密值>'
python .\recommendation-data-product\scripts\build_recommendation_sample.py `
  --source 'D:\测试数据\ads_zzdnew_recommend_dcn_wide_dm.csv' `
  --output-dir 'D:\synapxnet\.data\recommendation' `
  --target-rows 80000 `
  --positive-ratio 0.5 `
  --product-version 'recommendation-dcn-demo-v1'
```

构建结果包含三张原始层 CSV、一张训练产品 CSV 和 `manifest.json`。清单保存源记录摘要、Schema 摘要、文件摘要、标签分布和训练集划分，不保存盐值与个人信息。

## PostgreSQL 接入顺序

1. 使用 `sql/001_raw_schema.sql` 创建原始层与数据产品层。
2. 使用 `sql/003_security_roles.sql` 创建 DataOps 写入与 MLOps 只读角色。
3. 按用户、内容、交互顺序导入三张原始层 CSV。
4. 设置 `synapxnet.product_version` 后执行 `sql/002_build_training_product.sql`。
5. 校验记录数、正负标签和外键完整性。
6. 将 `manifest.json` 的 Schema 与制品摘要登记到数据产品注册表。
7. 只有状态为 `published` 的版本允许 XnetMLOps 导入。

DataOps 数据源账户默认只读。执行聚合和发布的账户应单独授权，并通过环境变量或秘密管理服务注入密码。

部署系统应分别创建登录账户并授予角色，不得让平台服务使用 PostgreSQL 超级用户：

```sql
CREATE ROLE recommendation_dataops LOGIN PASSWORD '<由秘密管理服务注入>';
GRANT xnet_dataops_product_writer TO recommendation_dataops;
CREATE ROLE recommendation_mlops_reader LOGIN PASSWORD '<由秘密管理服务注入>';
GRANT xnet_mlops_product_reader TO recommendation_mlops_reader;
```

## 测试

```powershell
python -m unittest discover .\recommendation-data-product\tests -v
```
