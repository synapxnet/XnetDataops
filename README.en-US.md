<!--
Copyright (C) 2026 Synapxnet. All rights reserved.
DataOps 版本、接入与复现说明 / DataOps version, access and reproduction guide.
Author: maoyo | Department: 研发部 | Date: 2026-09-18 | Version: 1.3.0
Maintainer: maoyo
-->

# XnetDataOps

[![GOAI release](https://img.shields.io/badge/GOAI%20release-1.3.0-1677ff.svg)](https://github.com/synapxnet/XnetDataops/releases/tag/v1.3.0)

[简体中文](README.md) · [English](README.en-US.md) · [日本語](README.ja-JP.md)

DataOps backend for ingestion, development, scheduling, quality, lineage, assets, audit and governed collaboration.

**[Pinned v1.3.0 source](https://github.com/synapxnet/XnetDataops/tree/v1.3.0) · [Release/downloads](https://github.com/synapxnet/XnetDataops/releases/tag/v1.3.0) · [Matching frontend](https://github.com/synapxnet/XnetDataops-web/tree/v1.3.0) · [OpenXnet installer](https://github.com/synapxnet/OpenXnet/releases/tag/v1.3.0)**

This default `display` branch retains historical showcase code. The GOAI release badge links to a separate release: clone the pinned tag below to reproduce it. README-only changes do not move that tag, replace source archives or redeploy services.

## Current demo and sign-in

- Staging: [https://goai.xnetdataops.synapxnet.online/](https://goai.xnetdataops.synapxnet.online/); [login](https://goai.xnetdataops.synapxnet.online/#/auth/login).
- Public demo phone: **`17870171303`**. Six-digit demo verification code: **`000000`**. These are for the authorized demo environment only, not password-based login.
- Verified on 2026-09-18: sign-in and read-only identity/status succeeded; user `goai_operator`, role `DEVELOPER`; resident platform `dataops`, Agent `1.3.0`, `ONLINE`, model/tools configured. This check did not execute business changes or a complete cross-platform run.

The [current authentication implementation](https://github.com/synapxnet/XnetDataops/blob/v1.3.0/dataops-usr-service/src/main/java/com/synapxnet/dataopsusrservice/service/impl/AuthServiceImpl.java) accepts pre-provisioned demo identities. The send-code endpoint returns a demo marker; **it does not deliver real SMS**. Use the demo code above directly. Self-registration is not implemented and QR login is unavailable. Production deployments must replace demo authentication and provision their own users and organization permissions.

This code authenticates the DataOps website only. It is **not an AgentTeams access code or Live execution authorization**; those belong to separate OpenXnet workspace controls. No model API keys or internal credentials are published here.

## API and resident Agent

| Purpose | Same-origin browser path | Meaning |
|---|---|---|
| User/auth | `/api`, including `/api/login` and `/api/user/info` | Gateway maps to USR `/api/usr`; do not append `/usr` again |
| Business services | `/dsm`, `/dim`, `/ddv`, `/tsk`, `/dqm`, `/dgv`, `/das`, `/dap`, `/dms`, `/dob`, `/dau` | Organization/team/resource authorization required |
| Governance workbench | `/dgv/governance/workbench` | Current deployment is a separate read-only route |
| Resident Agent | `/api/resident/v1/`; status: `/api/resident/v1/status` | Platform sign-in and server-side capability checks |

Runtime configuration was verified as `VITE_GLOB_API_URL=/api`. Anonymous resident/organization/workbench requests are rejected. An unauthenticated `/api/user/info` request can return HTTP 200 with a business error: inspect `code`, not HTTP status alone.

The [resident service](https://github.com/synapxnet/OpenXnet/tree/v1.3.0/services/platform-resident-agent) runs separately from Java services and AgentTeams. It provides local chat, allowed tools and task views. Its current handoff status is `handoffAvailable=false / PENDING_INTEGRATION`; automatic resident-to-cross-platform handoff must not be advertised as available. Cross-platform demos use the existing OpenXnet/AgentTeams integration. Internal `/health` is not independently exposed by the reviewed public gateway; use the authenticated status endpoint.

## Version scope and evidence

The repository has 12 business services plus the shared agent-contract module. It includes governance scope checks, DAG persistence, API-key controls, approval digests, idempotency and resource-version checks. All 14 Maven reactor items built, 76 tests passed and one real-database test was conditionally skipped. Competition-state backfill is not production bulk backfill; one-time migration is not continuous checkpointing.

See the [pinned source delivery guide](https://github.com/synapxnet/XnetDataops/blob/v1.3.0/docs/GOAI-V1.3.0-SOURCE-DELIVERY.md). Source release, documentation changes and read-only checks do not prove all deployed components match one source commit.

## Build the pinned version

```bash
git clone --branch v1.3.0 --single-branch https://github.com/synapxnet/XnetDataops.git
cd XnetDataops
mvn -B -ntp clean verify
```

Requires **JDK 17 and Maven 3.9.11**. Stack: Spring Boot 3.4.6, MyBatis 3.0.4, MySQL and Redis; the optional recommendation data product has additional PostgreSQL/data dependencies.

Before starting services, supply environment-specific database/Redis addresses, independent secrets and JWT configuration using each module's `application.properties`. Review `sql/xnet_dataops_ddl.sql` and `database/migrations`. Configure explicit organization/resource scopes through `config/governance-workbench.example.properties` and governed interfaces through `config/finals-agent.example.properties`; empty scopes grant no access. The resident service needs separate identity/model/tool/workspace configuration; its declaration is not an installable Kubernetes CRD.

The bundled Compose/Nginx files are historical development topology, not a verified one-command finals deployment. The old frontend Dockerfile still uses Node 18, conflicting with the current frontend's Node 20.10+ requirement. Network targets, organization checks and precise governance routes require adjustment. Build the matching frontend by its versioned instructions and validate deployment configuration in isolation; do not apply the generic files over an existing environment.

## Historical image

This image shows an earlier showcase layout, not the current v1.3.0 UI or acceptance evidence.

![Historical DataOps overview](docs/images/xnetdataops-overview.png)

## License and contributions

See [LICENSE](LICENSE) and component-specific notices. The frontend is based on [Vue Vben Admin](https://github.com/vbenjs/vue-vben-admin), retaining upstream attribution and licensing. Include a version, reproduction steps and redacted evidence in Issues/PRs; never include private credentials.
