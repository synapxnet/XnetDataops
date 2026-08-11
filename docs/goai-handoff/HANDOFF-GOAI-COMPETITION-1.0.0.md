# GOAI Competition 1.0.0 后端交接

- 仓库：`synapxnet/XnetDataops`
- 任务：`DATAOPS-BE-01`、`DATAOPS-BE-02`
- Worktree：`D:\synapxnet\.codex-build\goai-competition-1.0.0\XnetDataops`
- 分支：`GOAI-Competition`
- 基线提交：`f03a82b8af9c802bd71776c8ee790c8c014db267`
- 结束提交：见 `GOAI-Competition` 分支候选 HEAD
- 产品/契约版本：`1.0.0`

## 交付范围

| 工具 | 服务 | 固定演示事实 |
| --- | --- | --- |
| `dataops.quality.report.get` | DQM | `qr_risk_features_120`，120/128 契约检查失败 |
| `dataops.schema.snapshot.get` | DGV | `schema_risk_features_120`，120 字段和稳定 SHA-256 |
| `dataops.lineage.get` | DGV | 有界 BFS、环检测、稳定排序和截断 |
| `dataops.workflow.instance.get` | TSK | `task_risk_features_latest`，输出资产/Schema 与脱敏日志 |

公共 `dataops-agent-contract` 提供 1.0.0 包络、上下文和单工具委托令牌；DAU/DOB 迁移提供审计回执镜像和 Incident/Trace 关联能力。

## 迁移与回退

按顺序由唯一 migrator 执行：

```text
database/migrations/V20260802_01__dgv_schema_snapshot.sql
database/migrations/V20260802_02__dqm_contract_check.sql
database/migrations/V20260802_03__dau_agent_audit_receipt.sql
database/migrations/V20260802_04__dob_incident_link.sql
database/migrations/V20260802_05__goai_fixture.sql
database/migrations/V20260802__validate.sql
```

执行前备份并确认共享 Schema 归属。回退前导出 Evidence/Audit，再在停机窗口执行 `database/migrations/R20260802__goai_rollback.sql`。该脚本会删除新增列、索引和表，属于破坏性回退，不得与在线写入并发。

## 配置键

- `openxnet.agent.delegation-secret`
- `openxnet.agent.audience`
- `openxnet.internal.audit-token`（仅内部审计镜像调用）
- 各服务原有数据库连接配置

所有值由部署环境注入，仓库不提供默认密钥。

## 调用样例

```bash
curl -X POST 'https://<dataops-dgv>/api/agent/v1/tools/dataops.schema.snapshot.get:invoke' \
  -H 'Authorization: Bearer <short-lived-delegation-token>' \
  -H 'Content-Type: application/json' \
  -H 'X-OpenXnet-Workspace-Id: ws_goai_demo' \
  -H 'X-OpenXnet-Incident-Id: inc_model_contract_001' \
  -H 'X-OpenXnet-Trace-Id: trace_model_contract_001' \
  -H 'X-OpenXnet-Tool-Name: dataops.schema.snapshot.get' \
  -d '{"requestId":"req_schema_001","toolName":"dataops.schema.snapshot.get","arguments":{"assetUid":"asset_risk_features_prod","schemaVersion":"latest"},"dryRun":false}'
```

其余路径：

- `POST /api/agent/v1/tools/dataops.quality.report.get:invoke`
- `POST /api/agent/v1/tools/dataops.lineage.get:invoke`
- `POST /api/agent/v1/tools/dataops.workflow.instance.get:invoke`

## 验证记录

```powershell
mvn -pl dataops-dqm-service,dataops-dgv-service,dataops-tsk-service,dataops-dob-service,dataops-dau-service -am test
```

结果：`BUILD SUCCESS`；7 项新增测试通过，覆盖委托令牌、Schema 排序、血缘环/截断、质量报告和工作流输出/日志控制，0 失败、0 错误。

## GOAI Competition 1.1.0 三场景扩展

比赛环境入口为 `https://goai.xnetdataops.synapxnet.online`，当前解析到比赛专用服务器 `150.109.120.15`。域名和 IP 只属于部署环境，代码必须继续通过配置注入端点。

XnetDataops 当前负责 7 个固定工具：质量报告、Schema、血缘、工作流实例、训练数据集构建、特征回填和数据集验证。训练数据集构建与特征回填属于计划级写步骤，必须校验审批、参数摘要、资源版本、职责分离、幂等和补偿范围；数据集验证始终保持只读。

本轮定向测试：Agent Contract 9 项、Task 4 项、Data Quality 3 项通过。真实 HTTPS Live 已覆盖推荐工作流证据、量化盘后数据集构建和跨域特征回填/验证，并在测试后清理比赛进程状态。

权威工具契约位于 `contracts/goai-tools.v1.json`，由 `D:\synapxnet\scripts\Sync-GoaiCompetitionContracts.cjs` 从 OpenXnet 注册表生成。不得手工删除新增工具或恢复旧的 10 工具清单。

## 已知限制与后续注意

- 当前仓库没有统一 Flyway 启动器，部署方必须指定唯一 migrator。
- `includeLogSummary=false` 时不会读取日志；开启后仍执行字段脱敏和长度限制。
- 前端不得硬编码 120/128，结论必须来自四个 ToolResponse。
- 前端深链为 `XnetDataops-web` 的 `/agent/incidents/:incidentId/data-evidence`。
