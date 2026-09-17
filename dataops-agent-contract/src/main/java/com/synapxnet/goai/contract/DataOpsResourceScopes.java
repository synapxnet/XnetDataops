/*
Copyright (C) 2026 Synapxnet. All rights reserved.
This file is Synapxnet Proprietary and Confidential. It is strictly
forbidden to copy, distribute, or use without explicit authorization.
用途：将可信身份映射到明确资源授权。Purpose: Map verified identities to explicit resource scopes.
Author: maoyo | Department: 研发部 | Date: 2026-09-13
Version: 1.0.0 | Security Level: INTERNAL
__version__: 1.0.0 | __author__: maoyo | __copyright__: Copyright 2026 Synapxnet
__maintainer__: maoyo | __email__: synapxnet@gmail.com
 */
package com.synapxnet.goai.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DataOpsResourceScopes {
    private final List<Scope> scopes;
    private final boolean valid;

    /** 校验服务端映射，错误配置不授予资源。 Validate server-owned grants and fail closed on malformed configuration. */
    public DataOpsResourceScopes(String json, ObjectMapper mapper) {
        List<Scope> parsed = List.of();
        boolean accepted = false;
        try {
            parsed = mapper.readValue(json, new TypeReference<List<Scope>>() { });
            if (parsed == null || parsed.size() > 100) throw new IllegalArgumentException();
            Set<String> ids = new HashSet<>();
            Set<String> workspaces = new HashSet<>();
            Set<List<String>> organizations = new HashSet<>();
            for (Scope scope : parsed) {
                validateScope(scope);
                if (!ids.add(scope.scopeId()) || !workspaces.add(scope.workspaceId())
                        || !organizations.add(List.of(scope.tenantUid(), scope.deptUid(), scope.teamUid()))) {
                    throw new IllegalArgumentException();
                }
            }
            parsed = List.copyOf(parsed);
            accepted = true;
        } catch (Exception ignored) {
            parsed = List.of();
        }
        scopes = parsed;
        valid = accepted;
    }

    /** 只从验证后的委托工作区解析范围。 Resolve a scope from an already verified delegated workspace. */
    public Scope forWorkspace(String workspaceId) {
        requireConfigured();
        return scopes.stream().filter(scope -> scope.workspaceId().equals(workspaceId)).findFirst()
                .orElseThrow(() -> new AgentContractException(403, "SCOPE_FORBIDDEN", "当前工作区没有资源授权映射"));
    }

    /** 只在USR认证成功后按组织三元组解析范围。 Resolve one scope after the trusted USR service confirms organization access. */
    public Scope forOrganization(String tenant, String department, String team) {
        requireConfigured();
        return scopes.stream().filter(scope -> scope.tenantUid().equals(tenant)
                && scope.deptUid().equals(department) && scope.teamUid().equals(team)).findFirst()
                .orElseThrow(() -> new AgentContractException(403, "SCOPE_FORBIDDEN", "当前组织没有资源授权映射"));
    }

    /** 空配置不回退全库读取。 Reject missing grants instead of falling back to unrestricted reads. */
    private void requireConfigured() {
        if (!valid || scopes.isEmpty()) {
            throw new AgentContractException(503, "RESOURCE_SCOPE_UNAVAILABLE", "尚未配置有效的数据资源范围");
        }
    }

    /** 校验身份、模式和有界UID集合。 Validate identity fields, explicit execution mode and bounded resource lists. */
    private static void validateScope(Scope scope) {
        if (scope == null) throw new IllegalArgumentException();
        for (String value : List.of(scope.scopeId(), scope.workspaceId(), scope.tenantUid(), scope.deptUid(), scope.teamUid())) {
            if (value.isBlank() || value.length() > 128) throw new IllegalArgumentException();
        }
        if (!Set.of("live", "simulation", "replay", "fixture").contains(scope.executionMode())) throw new IllegalArgumentException();
        for (List<String> values : List.of(scope.assetUids(), scope.reportUids(), scope.instanceUids())) {
            if (values.size() > 100) throw new IllegalArgumentException();
            for (String value : values) {
                if (value == null || value.isBlank() || value.length() > 128) throw new IllegalArgumentException();
            }
        }
    }

    public record Scope(String scopeId, String workspaceId, String tenantUid, String deptUid,
                        String teamUid, String executionMode, List<String> assetUids,
                        List<String> reportUids, List<String> instanceUids) {
        /** 固化资源集合，防止授权对象被调用者改写。 Freeze resource lists so callers cannot mutate grants. */
        public Scope {
            assetUids = assetUids == null ? List.of() : List.copyOf(assetUids);
            reportUids = reportUids == null ? List.of() : List.copyOf(reportUids);
            instanceUids = instanceUids == null ? List.of() : List.copyOf(instanceUids);
        }
        /** 检查资产是否明确授权。 Check whether the asset UID is explicitly granted. */
        public boolean allowsAsset(String uid) { return uid != null && assetUids.contains(uid); }
        /** 未授权资产使用不可枚举错误。 Reject ungranted assets without exposing their existence. */
        public void requireAsset(String uid) { require(assetUids, uid); }
        /** 检查明确的报告授权。 Require an explicit quality-report grant. */
        public void requireReport(String uid) { require(reportUids, uid); }
        /** 检查明确的实例授权。 Require an explicit workflow-instance grant. */
        public void requireInstance(String uid) { require(instanceUids, uid); }
        /** 统一资源拒绝语义。 Use the same non-enumerating error for absent resource grants. */
        private static void require(List<String> values, String uid) {
            if (uid == null || !values.contains(uid)) throw new AgentContractException(404, "RESOURCE_NOT_FOUND", "当前范围内未找到资源");
        }
    }
}
