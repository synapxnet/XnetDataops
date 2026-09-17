<!--
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
决赛源码交付 / Finals source delivery.
Author: maoyo | Department: 研发部 | Date: 2026-09-17 | Version: 1.3.0
Security Level: INTERNAL | Maintainer: maoyo | Email: synapxnet@gmail.com
-->

# XnetDataOps GOAI V1.3.0 后端源码交付

这是当前决赛源码的整合提交，不是重新部署线上服务。产品、父 POM 和十三个 Java 模块统一为 1.3.0；工具响应和治理工作台的契约 schema 保留兼容版本。前端位于 XnetDataops-web 的 GOAI-Competition 分支。

## 来源与范围

| 来源 | 本次保留的内容 |
| --- | --- |
| GitHub GOAI 基线 `43c2f77474580af4721affd029bfa700dfee0767` | 原十二服务、用户组织认证、推荐数据产品、部署与建表脚本 |
| `goai-finals-dataops/backend`，2026-09-15 治理发布工作树 | 治理工作台、资源作用域和组织身份校验、只读取证、API Key 管理员校验与脱敏、DAG 持久化、数据库迁移、回归测试 |
| `source-repair/XnetDataops`，2026-09-15 已部署修订 | 共享资源版本跟踪器及迁移测试；TSK 数据集受控执行、工作流版本读取、量化数据产品读取及测试，共 10 个 Java 文件 |
| DataOps 根工作树 | `deploy/resident-agent-v1.3.0.yaml` 的驻场身份与只读工具声明 |

没有使用较早的 source-repair 工作树覆盖较新的组织作用域、DAG 和 API Key 修复。驻场服务独立运行；实际实现固定引用 [OpenXnet c841ef84](https://github.com/synapxnet/OpenXnet/tree/c841ef841da8477fc312e27cd390aecac8ed2d7e/services/platform-resident-agent)。驻场模型 URL/API Key 在服务端独立配置，不放入本仓库，也不由网页回读。

## 构建与验证

使用 JDK 17 和 Maven 3.9.11：`mvn -B -ntp clean verify`。2026-09-17 在隔离源码副本完成全部 14 项 Reactor 构建；76 项测试通过，1 项依赖真实推荐数据库的集成测试按既有条件跳过，无失败。覆盖委托认证、审批摘要、版本与迁移、DGV 治理作用域、TSK DAG、DQM 取证和 DAP 密钥边界。未连接线上数据库执行变更，未启动生产服务。

移除旧提交中跟踪的 `target/`、IDE 元数据，保留正式 Java/SQL/配置/测试；安装包、运行快照、数据集、日志和真实凭据不随源码交付。JAR 名称为 `dataops-*-service-1.3.0.jar`，既有后端 Dockerfile 使用 `target/*.jar`，无需保留旧文件名。

## 运行与部署边界

- `config/governance-workbench.example.properties` 定义工作空间、组织、资产和证据范围；无已授权映射时默认拒绝，不把有响应等同于已授权。
- `config/finals-agent.example.properties` 说明委托、审批、量化产品和迁移项；部署者通过受保护配置注入真实值。量化连接缺失时对应读取不可用，不影响其他工作流启动。
- `deploy/resident-agent-v1.3.0.yaml` 是部署契约声明，不是安装 Kubernetes CRD 的可执行文件。需要配置同版本驻场服务以及 `/api/resident/v1` 代理和原平台登录验证。
- 仓库旧 Compose/Nginx 是通用开发拓扑，不能替代线上按路由切分的发布清单。2026-09-15 线上只将 `/dgv/governance/` 接入治理修复服务，保留原 DGV 取证与 TSK 写上游；前端 `/dsm`、`/tsk`、`/dgv` 等前缀和驻场入口需按真实运行服务配置。
- `CompetitionDatasetToolController` 的特征回填路径维护受控比赛状态，不代表对企业生产数据做了真实批量回填。量化数据证据需真实 PostgreSQL 的已发布研究数据产品。固定比赛工作流回退明确带沙盘来源标记，未知 UID 仍拒绝。
- TSK 资源迁移使用匹配摘要的一次性导出和 consumed 标记；当前实现不等于持续持久化检查点。重启/版本回退前需要保存并审阅当前状态，不可删除 consumed 标记重放旧状态。AIOps 的独立检查点机制不能被当作 DataOps 已具备相同恢复能力。
- 本轮构建与单测证明最终整合源码可编译且所列边界测试通过，不代替线上验证码、短信、真实数据同步、写操作、完整 AgentTeams 成功/失败演练验收。

## 回退

GitHub 使用正常快进提交，没有改写既有分支历史。源码回退可基于此前 GOAI Commit；线上回退必须使用对应版本 JAR、路由与当前数据状态的受审阅备份，不能仅回退源码或重建所有容器。
