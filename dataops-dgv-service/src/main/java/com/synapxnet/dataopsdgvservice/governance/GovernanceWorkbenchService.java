/*
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：组合只读数据治理工作台。Purpose: Assemble the scoped native governance workbench.
Author: maoyo | Department: 研发部 | Date: 2026-09-13
Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.dataopsdgvservice.governance;

import com.synapxnet.dataopsdgvservice.agent.AgentDgvEvidenceService;
import com.synapxnet.dataopsdgvservice.agent.AgentDgvMapper;
import com.synapxnet.dataopsdgvservice.entity.MetaTable;
import com.synapxnet.goai.contract.AgentContractException;
import com.synapxnet.goai.contract.DataOpsResourceScopes;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class GovernanceWorkbenchService {
    private final AgentDgvMapper catalog;
    private final AgentDgvEvidenceService evidence;
    private final GovernanceWorkbenchMapper quality;

    /** 注入只读领域依赖。 Inject read-only domain sources without an execution service. */
    public GovernanceWorkbenchService(AgentDgvMapper catalog, AgentDgvEvidenceService evidence, GovernanceWorkbenchMapper quality) {
        this.catalog = catalog; this.evidence = evidence; this.quality = quality;
    }

    /** 在授权范围内组合事实，各源失败单独披露。 Assemble granted facts and disclose source failures independently. */
    public Workbench read(DataOpsResourceScopes.Scope scope, String assetUid, String search, String direction, int maxDepth) {
        if (search != null && search.length() > 100) throw new AgentContractException(400, "INVALID_ARGUMENT", "搜索内容过长");
        if (maxDepth < 1 || maxDepth > 5) throw new AgentContractException(400, "INVALID_ARGUMENT", "关系深度应为1到5");
        AgentDgvEvidenceService.Direction selectedDirection;
        try { selectedDirection = AgentDgvEvidenceService.Direction.valueOf(direction.toUpperCase(Locale.ROOT)); }
        catch (Exception exception) { throw new AgentContractException(400, "INVALID_ARGUMENT", "关系方向无效"); }
        boolean explicitTarget = assetUid != null && !assetUid.isBlank();
        if (explicitTarget) scope.requireAsset(assetUid);
        List<Asset> assets = new ArrayList<>(); List<SourceStatus> sources = new ArrayList<>();
        List<Asset> availableAssets = new ArrayList<>();
        boolean catalogAvailable = false;
        try {
            for (String uid : scope.assetUids()) {
                MetaTable table = catalog.findTableByUid(uid);
                if (table != null) {
                    String name = table.getSchemaName() + "." + table.getTableName();
                    Asset asset = new Asset(uid, name, table.getTableType(), table.getRowCount());
                    availableAssets.add(asset);
                    if (search == null || name.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))) {
                        assets.add(asset);
                    }
                }
            }
            catalogAvailable = true;
            sources.add(new SourceStatus("catalog", availableAssets.isEmpty() ? "empty" : "available", "已授权目录"));
        } catch (Exception exception) {
            assets.clear(); sources.add(new SourceStatus("catalog", "unavailable", "目录源暂不可用"));
        }
        // 显式目标独立于搜索结果，缺失时拒绝，禁止改选另一份证据。 Keep explicit targets independent from search and never substitute another asset.
        if (catalogAvailable && explicitTarget && availableAssets.stream().noneMatch(asset -> asset.uid().equals(assetUid))) {
            throw new AgentContractException(404, "RESOURCE_NOT_FOUND", "指定资产不存在");
        }
        String selected = !catalogAvailable ? null : explicitTarget ? assetUid : assets.isEmpty() ? null : assets.get(0).uid();
        AgentDgvEvidenceService.LineageEvidence lineage = null;
        AgentDgvEvidenceService.SchemaSnapshotEvidence schema = null;
        List<GovernanceWorkbenchMapper.QualityRow> reports = new ArrayList<>();
        if (selected != null) {
            try {
                lineage = evidence.getLineage(selected, selectedDirection, maxDepth, scope::allowsAsset);
                sources.add(new SourceStatus("lineage", "available", "已登记表级关系"));
            } catch (Exception exception) { sources.add(new SourceStatus("lineage", "unavailable", "关系源暂不可用")); }
            try {
                schema = evidence.getSchemaSnapshot(selected, null, null);
                sources.add(new SourceStatus("schema", "available", "持久化Schema版本"));
            } catch (AgentContractException exception) {
                sources.add(new SourceStatus("schema", exception.getHttpStatus() == 404 ? "empty" : "unavailable", "未取得Schema快照"));
            } catch (Exception exception) { sources.add(new SourceStatus("schema", "unavailable", "Schema源暂不可用")); }
            try {
                for (String uid : scope.reportUids()) {
                    GovernanceWorkbenchMapper.QualityRow report = quality.quality(uid);
                    if (report != null && selected.equals(report.assetUid()) && scope.allowsAsset(report.assetUid())) reports.add(report);
                }
                sources.add(new SourceStatus("quality", reports.isEmpty() ? "empty" : "available", "已授权且有持久化资产关联的报告"));
            } catch (Exception exception) { reports.clear(); sources.add(new SourceStatus("quality", "unavailable", "质量证据源暂不可用")); }
        }
        String availability = sources.stream().anyMatch(source -> source.availability().equals("available")) ? "available"
                : sources.stream().anyMatch(source -> source.availability().equals("unavailable")) ? "unavailable" : "empty";
        return new Workbench("1.0.0", UUID.randomUUID().toString(), Instant.now(), "dataops", "native",
                scope.executionMode(), availability, new ScopeView(scope.scopeId(), scope.tenantUid(), scope.deptUid(), scope.teamUid()),
                List.copyOf(assets), selected, lineage, schema, List.copyOf(reports), List.copyOf(sources),
                new Capabilities(true, false), List.of("仅查看原生记录，不执行治理动作", "Schema快照版本不等于模型或数据产品发布版本", "质量检查时间按源数据库配置解释"));
    }

    public record Asset(String uid, String name, String kind, Long rowCount) { }
    public record SourceStatus(String source, String availability, String summary) { }
    public record ScopeView(String scopeId, String tenantUid, String deptUid, String teamUid) { }
    public record Capabilities(boolean readOnly, boolean execute) { }
    public record Workbench(String schemaVersion, String requestId, Instant capturedAt, String sourcePlatform,
                            String sourceOrigin, String executionMode, String availability, ScopeView scope,
                            List<Asset> assets, String selectedAssetUid, AgentDgvEvidenceService.LineageEvidence lineage,
                            AgentDgvEvidenceService.SchemaSnapshotEvidence schema, List<GovernanceWorkbenchMapper.QualityRow> quality,
                            List<SourceStatus> sources, Capabilities capabilities, List<String> limitations) { }
}
