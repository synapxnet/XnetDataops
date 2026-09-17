<!--
Copyright (C) 2026 Synapxnet. All rights reserved.
DataOps 版本、接入与复现说明 / DataOps version, access and reproduction guide.
Author: maoyo | Department: 研发部 | Date: 2026-09-18 | Version: 1.3.0
Maintainer: maoyo
-->

# XnetDataOps

[![GOAI release](https://img.shields.io/badge/GOAI%20release-1.3.0-1677ff.svg)](https://github.com/synapxnet/XnetDataops/releases/tag/v1.3.0)

数据工程与治理平台后端，提供数据接入、开发、调度、质量、血缘、资产及审计接口，并接入受控跨平台协作。

**[v1.3.0 固定源码](https://github.com/synapxnet/XnetDataops/tree/v1.3.0) · [发布页](https://github.com/synapxnet/XnetDataops/releases/tag/v1.3.0) · [源码 ZIP](https://github.com/synapxnet/XnetDataops/releases/download/v1.3.0/XnetDataops-v1.3.0-source.zip) · [配套前端 v1.3.0](https://github.com/synapxnet/XnetDataops-web/tree/v1.3.0) · [OpenXnet 安装包](https://github.com/synapxnet/OpenXnet/releases/tag/v1.3.0)**

复现统一使用 `v1.3.0` 标签或本页对应的源码 ZIP；当前版本说明随源码提供。本次发布文档整理不会重新部署线上服务。

## 当前演示入口与登录

| 项目 | 已核验入口或说明 |
|---|---|
| DataOps Staging | [https://goai.xnetdataops.synapxnet.online/](https://goai.xnetdataops.synapxnet.online/) |
| 登录页 | [手机号验证码登录](https://goai.xnetdataops.synapxnet.online/#/auth/login) |
| 公开演示手机号 | `17870171303` |
| 六位演示验证码 | `000000`，仅用于已授权演示环境 |
| 登录方式 | 手机号 + 演示验证码，不是账号密码登录 |
| 当前演示身份 | `goai_operator`，角色 `DEVELOPER`；页面和工具权限仍由服务端决定 |

2026-09-18 已实际完成上述演示登录、身份读取和驻场状态读取：返回平台 `dataops`、Agent 版本 `1.3.0`、`ONLINE`，模型及工具配置已就绪。只执行登录和只读检查，没有运行数据变更、训练或跨平台执行。公开演示环境可能维护，网页可达不等于每条业务链可执行。

[当前认证实现](https://github.com/synapxnet/XnetDataops/blob/v1.3.0/dataops-usr-service/src/main/java/com/synapxnet/dataopsusrservice/service/impl/AuthServiceImpl.java)仅接受预置演示身份；发送验证码入口返回演示标记，**没有接入真实短信投递**。因此请直接使用上述演示验证码，不要把界面“验证码已发送”当作真实短信到达证明。账号开通由管理员处理；扫码登录尚未接入。正式部署需更换演示认证并配置自己的用户、组织与权限，不应直接公开演示认证服务。

此验证码只用于 DataOps 网页登录，**不是 AgentTeams 演示访问码，也不是 Live 执行授权**。后两项按 OpenXnet 工作空间另行配置；模型 API Key、服务端委托凭据不会在 README 提供。

## API 与驻场 Agent

| 用途 | 浏览器同源路径 | 边界 |
|---|---|---|
| 用户认证与身份 | `/api`，如 `/api/login`、`/api/user/info` | 网关转发到 USR 的 `/api/usr`，不要在浏览器再重复拼接 `/usr` |
| 原生业务 | `/dsm`、`/dim`、`/ddv`、`/tsk`、`/dqm`、`/dgv`、`/das`、`/dap`、`/dms`、`/dob`、`/dau` | 按组织、团队和资源范围授权 |
| 治理工作台 | `/dgv/governance/workbench` | 当前线上是独立只读路由，不能据此推断拥有治理写权限 |
| 驻场服务 | `/api/resident/v1/`，状态为 `/api/resident/v1/status` | 需要平台登录；服务端判断配置和工具权限 |

线上运行配置已核验 `VITE_GLOB_API_URL=/api`。未登录读取驻场状态、组织树和治理工作台均被拒绝；`/api/user/info` 的未认证响应可能是 HTTP 200 包裹业务错误，必须同时检查业务 `code`，不能只看 HTTP 200 就判定健康。

驻场 Agent 使用[共享独立服务](https://github.com/synapxnet/OpenXnet/tree/v1.3.0/services/platform-resident-agent)，不等同于 Java 业务进程，也不等同于 AgentTeams 团队。它能够在平台内聊天、读取已授权工具和展示任务；本次状态回包的自动跨域移交仍为 `handoffAvailable=false / PENDING_INTEGRATION`，不能宣称点击移交已接通真实跨域编排。跨平台演示由 OpenXnet/AgentTeams 的既有接入链组织。服务内部 `/health` 没有在已核验公网网关中单独开放，公网应使用需登录的状态接口。

## v1.3.0 能力与验证范围

- 12 个业务服务加 `dataops-agent-contract` 契约模块；包括治理工作台、DAG 持久化、API Key 权限与脱敏、资源作用域和组织校验。
- 审批摘要、幂等、共享资源版本与受控 TSK 操作；驻场服务在平台侧独立运行。
- Maven **14 个 Reactor 构建成功**（含父项目），**76 项测试通过、1 项真实数据库测试条件跳过**。
- TSK 比赛状态回填不是企业生产数据的真实批量回填；一次性资源迁移也不是持续检查点。

完整来源、测试和限制见 [v1.3.0 源码交付说明](https://github.com/synapxnet/XnetDataops/blob/v1.3.0/docs/GOAI-V1.3.0-SOURCE-DELIVERY.md)。源码发布、README 更新和本次只读接入核验都不意味着线上所有组件已重新部署到同一个源码提交。 构建与测试对应[程序验证基线 ac638e05](https://github.com/synapxnet/XnetDataops/commit/ac638e05b02f69e4085a685e6e08ea5d63723f6c)；之后的发布对齐只更新 README，程序文件与该基线一致。

## 固定版本构建

```bash
git clone --branch v1.3.0 --single-branch https://github.com/synapxnet/XnetDataops.git
cd XnetDataops
```

要求 **JDK 17、Maven 3.9.11**。后端使用 Spring Boot 3.4.6、MyBatis 3.0.4、MySQL 与 Redis；推荐数据产品扩展另需 PostgreSQL 等自己的数据依赖。

```bash
mvn -B -ntp clean verify
```

构建入口是 [pom.xml](https://github.com/synapxnet/XnetDataops/blob/v1.3.0/pom.xml)。构建通过不等于已配置可运行环境。启动前按模块 `application.properties` 提供数据库/Redis地址、独立凭据与 JWT 密钥，审阅 [初始表结构](https://github.com/synapxnet/XnetDataops/blob/v1.3.0/sql/xnet_dataops_ddl.sql) 及 [迁移](https://github.com/synapxnet/XnetDataops/tree/v1.3.0/database/migrations)，并建立真实组织授权映射。

治理配置入口为 [工作台范围示例](https://github.com/synapxnet/XnetDataops/blob/v1.3.0/config/governance-workbench.example.properties) 与 [决赛受控接口配置](https://github.com/synapxnet/XnetDataops/blob/v1.3.0/config/finals-agent.example.properties)。空映射默认不授权。驻场服务另外配置平台身份服务、模型、工具网关与工作空间委托；部署声明不是可直接安装的 Kubernetes CRD。

仓库 `docker-compose.yml` 与 Nginx 是通用开发拓扑，**不是已经验证的一键决赛部署**：旧前端 Dockerfile 仍基于 Node 18，与当前前端 Node 20.10+ 要求冲突；上游网络、组织鉴权和治理精确路由也需按环境调整。请先使用配套前端的版本化构建步骤，在隔离环境整理部署配置，不要原样覆盖现网。

## 历史截图

以下是历史展示版图片，仅供理解模块布局，不是 v1.3.0 当前 UI 或本次验收证据。

![DataOps 历史展示概览](docs/images/xnetdataops-overview.png)

## 许可与贡献

以本仓库 [LICENSE](LICENSE) 及组件各自声明为准。前端基于 [Vue Vben Admin](https://github.com/vbenjs/vue-vben-admin)，保留上游版权与许可。欢迎通过本仓库 Issues/PR 提交问题；请附版本、复现路径和脱敏证据，不附真实凭据。
