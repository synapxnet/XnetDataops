## GOAI finals release · v1.3.0

**[Release and source downloads](https://github.com/synapxnet/XnetDataops/releases/tag/v1.3.0) · [GOAI branch](https://github.com/synapxnet/XnetDataops/tree/GOAI-Competition) · [Matching frontend](https://github.com/synapxnet/XnetDataops-web/releases/tag/v1.3.0) · [OpenXnet installer](https://github.com/synapxnet/OpenXnet/releases/tag/v1.3.0)**

This default `display` branch retains historical showcase code. The GOAI release badge links to the separate finals release; it does not claim that this branch or deployed services were upgraded. Download the pinned version from the release page.

Version 1.3.0 integrates organization/resource governance, DAG persistence, approval digests, resource-version checks, and the resident Agent integration contract. All 14 Maven reactor items built; 76 tests passed and one real-database test was conditionally skipped. Competition-state backfill is not production bulk backfill, and one-time migration is not continuous checkpointing.

See the pinned [source delivery guide](https://github.com/synapxnet/XnetDataops/blob/ac638e05b02f69e4085a685e6e08ea5d63723f6c/docs/GOAI-V1.3.0-SOURCE-DELIVERY.md) for build instructions, dependencies and verification limits. This documentation update does not move the release tag or redeploy services.

---

<div align="center">

[简体中文](./README.md) | **English** | [日本語](./README.ja-JP.md)

# XnetDataops

**Open-source DataOps for integration, development, governance, and data services**

[![GOAI release](https://img.shields.io/badge/GOAI%20release-1.3.0-1677ff.svg)](https://github.com/synapxnet/XnetDataops/releases/tag/v1.3.0)
[![Java](https://img.shields.io/badge/Java-17-e76f00.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.6-6db33f.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-2ea44f.svg)](./LICENSE)

[Live Demo](https://www.xnetdataops.synapxnet.cn) · [Frontend: XnetDataops-web](https://github.com/synapxnet/XnetDataops-web) · [OpenXnet](https://openxnet.synapxnet.com) · [License](./LICENSE)

</div>

> The images below are historical showcase screenshots, not the current v1.3.0 UI or acceptance evidence.

![XnetDataops overview](./docs/images/xnetdataops-overview.png)

## Product Tour (historical screenshots)

| Demo login | Data source configuration |
| --- | --- |
| ![Demo login](./docs/images/xnetdataops-login.png) | ![Data sources](./docs/images/xnetdataops-datasource.png) |
| Data integration | SQL workbench |
| ![Integration](./docs/images/xnetdataops-integration.png) | ![SQL workbench](./docs/images/xnetdataops-workbench.png) |
| Workflow scheduling | Data quality |
| ![Workflows](./docs/images/xnetdataops-workflows.png) | ![Data quality](./docs/images/xnetdataops-quality.png) |
| Data lineage | Data APIs |
| ![Lineage](./docs/images/xnetdataops-lineage.png) | ![Data APIs](./docs/images/xnetdataops-api.png) |
| Data masking | Observability |
| ![Masking](./docs/images/xnetdataops-masking.png) | ![Observability](./docs/images/xnetdataops-observability.png) |
| Audit | About |
| ![Audit](./docs/images/xnetdataops-audit.png) | ![About](./docs/images/xnetdataops-about.png) |

## Overview

XnetDataops is an open-source, end-to-end DataOps platform maintained by the **SynapXnet team**. It manages the data lifecycle from source onboarding and transformation to scheduling, quality, governance, APIs, observability, and audit.

This backend repository and [XnetDataops-web](https://github.com/synapxnet/XnetDataops-web) form an enterprise-grade, multi-tenant, frontend/backend-separated system. Twelve modular services can be deployed together or integrated by domain.

## Highlights

- Enterprise multi-tenancy with user, role, team, and data boundaries.
- Complete flow across integration, development, scheduling, quality, governance, and delivery.
- Independent frontend and backend deployment.
- Extensible connectors, quality rules, scheduling nodes, APIs, and policies.
- Continuous improvements from the SynapXnet team.

## Modules

| Module | Service | Responsibility |
| --- | --- | --- |
| DSM | `dataops-dsm-service` | Data source connections and health |
| DIM | `dataops-dim-service` | Batch/incremental synchronization, mappings, and logs |
| DDV | `dataops-ddv-service` | SQL workbench, scripts, saved queries, and history |
| TSK | `dataops-tsk-service` | DAG workflows, dependencies, instances, and execution state |
| DQM | `dataops-dqm-service` | Quality rules, reports, and alerts |
| DGV | `dataops-dgv-service` | Metadata catalog, columns, lineage, and tags |
| DAS | `dataops-das-service` | Data assets, classification, statistics, and access records |
| DAP | `dataops-dap-service` | Data APIs, keys, rate limits, and call logs |
| DMS | `dataops-dms-service` | Masking rules, policies, and execution logs |
| DOB | `dataops-dob-service` | Freshness, volume, schema monitoring, and SLA |
| DAU | `dataops-dau-service` | Audit logs, data changes, and compliance reports |
| USR | `dataops-usr-service` | Authentication, users, roles, and access control |

## Quick Start

```bash
mvn -DskipTests package
cp .env.example .env
docker compose up -d --build
docker compose ps
```

Requirements: JDK 17+, Maven 3.9+, Docker Compose, MySQL 8.x, and Redis 7.x.

The idempotent showcase dataset is in `sql/xnet_dataops_demo.sql`. It uses non-routable addresses and invalid placeholder credentials, and preserves user-created records.

## Demo

- URL: <https://www.xnetdataops.synapxnet.cn>
- Phone: `12345678900`
- Verification code: `000000`

The fixed code is only for the public showcase. Production must use secure authentication.

## Community and License

XnetDataops is part of [OpenXnet](https://openxnet.synapxnet.com). Issues and pull requests are welcome.

Released under the [MIT License](./LICENSE). Copyright © 2026 SynapXnet.
