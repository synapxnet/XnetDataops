package com.synapxnet.dataopstskservice.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从隔离 PostgreSQL 读取真实 A 股因子数据产品和质量证据。
 */
@Service
public class QuantitativeDatasetProductService {

    private static final String PRODUCT_VERSION = "a-share-factor-demo-v1";
    private static final String USAGE_BOUNDARY = "RESEARCH_ONLY / SIMULATION_ONLY";

    private final String jdbcUrl;
    private final String username;
    private final String password;

    /**
     * 注入量化研究 PostgreSQL 连接配置。
     *
     * @param jdbcUrl PostgreSQL JDBC 地址
     * @param username DataOps 只读账号
     * @param password DataOps 只读账号密码
     */
    public QuantitativeDatasetProductService(
            @Value("${xnet.quantitative.jdbc-url}") String jdbcUrl,
            @Value("${xnet.quantitative.username}") String username,
            @Value("${xnet.quantitative.password}") String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * 判断数据集标识是否属于真实 A 股研究数据产品。
     *
     * @param datasetUid 数据集标识
     * @return 属于量化研究产品时返回 true
     */
    public boolean supports(String datasetUid) {
        return PRODUCT_VERSION.equals(datasetUid);
    }

    /**
     * 返回已发布数据产品的真实构建证据，不在工具层伪造或重算数据。
     *
     * @param datasetUid 数据产品版本
     * @param workflowInstanceUid DataOps 工作流实例
     * @param addedFactors 本次新增因子
     * @param removedFactors 本次移除因子
     * @return 可进入 AgentTeams 审计回执的数据产品证据
     */
    public Map<String, Object> buildEvidence(
            String datasetUid,
            String workflowInstanceUid,
            List<String> addedFactors,
            List<String> removedFactors) {
        ProductSnapshot snapshot = readSnapshot(datasetUid);
        Map<String, Object> data = baseEvidence(snapshot);
        data.put("workflowInstanceUid", workflowInstanceUid);
        data.put("addedFactors", List.copyOf(addedFactors));
        data.put("removedFactors", List.copyOf(removedFactors));
        data.put("status", "SUCCEEDED");
        data.put("buildMode", "VERSIONED_PUBLISHED_PRODUCT");
        return Map.copyOf(data);
    }

    /**
     * 返回数据产品的真实 Schema、样本划分、质量、血缘和用途边界。
     *
     * @param datasetUid 数据产品版本
     * @return 真实数据库查询得到的质量门证据
     */
    public Map<String, Object> validationEvidence(String datasetUid) {
        ProductSnapshot snapshot = readSnapshot(datasetUid);
        boolean valid = "published".equals(snapshot.status())
                && snapshot.rowCount() >= 1_000
                && snapshot.positiveCount() > 0
                && snapshot.negativeCount() > 0
                && snapshot.trainCount() > 0
                && snapshot.validationCount() > 0
                && snapshot.testCount() > 0;
        Map<String, Object> data = baseEvidence(snapshot);
        data.put("status", valid ? "VALID" : "NOT_READY");
        data.put("passed", valid);
        data.put("valid", valid);
        data.put("schemaCompatible", snapshot.schemaDigestSha256() != null);
        data.put("reproducible", snapshot.artifactDigestSha256() != null);
        data.put("datasetType", "A_SHARE_FACTOR_TRAINING");
        data.put("trainRows", snapshot.trainCount());
        data.put("validationRows", snapshot.validationCount());
        data.put("testRows", snapshot.testCount());
        return Map.copyOf(data);
    }

    /**
     * 查询已发布数据产品清单和训练表分割统计。
     *
     * @param datasetUid 数据产品版本
     * @return 不含证券名称、新闻和持仓的聚合快照
     */
    private ProductSnapshot readSnapshot(String datasetUid) {
        if (!supports(datasetUid)) {
            throw new IllegalArgumentException("不支持的量化数据产品: " + datasetUid);
        }
        String sql = """
                SELECT registry.product_version, registry.status, registry.row_count,
                       registry.positive_count, registry.negative_count, registry.symbol_count,
                       registry.source_date_from, registry.source_date_to,
                       registry.schema_digest_sha256, registry.artifact_digest_sha256,
                       registry.lineage_reference, registry.quality_score, registry.published_at,
                       COUNT(*) FILTER (WHERE training.dataset_split='train') AS train_count,
                       COUNT(*) FILTER (WHERE training.dataset_split='validation') AS validation_count,
                       COUNT(*) FILTER (WHERE training.dataset_split='test') AS test_count
                FROM quant_curated.data_product_registry AS registry
                INNER JOIN quant_curated.factor_training AS training
                    ON training.product_version=registry.product_version
                WHERE registry.product_version=?
                GROUP BY registry.product_version, registry.status, registry.row_count,
                         registry.positive_count, registry.negative_count, registry.symbol_count,
                         registry.source_date_from, registry.source_date_to,
                         registry.schema_digest_sha256, registry.artifact_digest_sha256,
                         registry.lineage_reference, registry.quality_score, registry.published_at
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, datasetUid);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("量化数据产品不存在: " + datasetUid);
                }
                return mapSnapshot(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("查询量化数据产品失败", exception);
        }
    }

    /**
     * 打开量化 PostgreSQL 连接并拒绝缺失运行时凭据。
     *
     * @return 已认证数据库连接
     * @throws SQLException PostgreSQL 连接失败时抛出
     */
    private Connection openConnection() throws SQLException {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("QUANTITATIVE_DATAOPS_DB_PASSWORD 未配置");
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**
     * 将 JDBC 聚合结果转换为不可变产品快照。
     *
     * @param resultSet 已定位到首行的查询结果
     * @return 量化数据产品快照
     * @throws SQLException 字段读取失败时抛出
     */
    private ProductSnapshot mapSnapshot(ResultSet resultSet) throws SQLException {
        return new ProductSnapshot(
                resultSet.getString("product_version"),
                resultSet.getString("status"),
                resultSet.getLong("row_count"),
                resultSet.getLong("positive_count"),
                resultSet.getLong("negative_count"),
                resultSet.getLong("symbol_count"),
                resultSet.getObject("source_date_from", LocalDate.class),
                resultSet.getObject("source_date_to", LocalDate.class),
                resultSet.getString("schema_digest_sha256"),
                resultSet.getString("artifact_digest_sha256"),
                resultSet.getString("lineage_reference"),
                resultSet.getDouble("quality_score"),
                resultSet.getObject("published_at", OffsetDateTime.class),
                resultSet.getLong("train_count"),
                resultSet.getLong("validation_count"),
                resultSet.getLong("test_count"));
    }

    /**
     * 构建所有量化数据工具共享的真实证据字段。
     *
     * @param snapshot 数据库快照
     * @return 保持插入顺序的可扩展证据映射
     */
    private Map<String, Object> baseEvidence(ProductSnapshot snapshot) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("datasetUid", snapshot.productVersion());
        data.put("productVersion", snapshot.productVersion());
        data.put("rowCount", snapshot.rowCount());
        data.put("positiveCount", snapshot.positiveCount());
        data.put("negativeCount", snapshot.negativeCount());
        data.put("symbolCount", snapshot.symbolCount());
        data.put("sourceDateFrom", snapshot.sourceDateFrom().toString());
        data.put("sourceDateTo", snapshot.sourceDateTo().toString());
        data.put("qualityScore", snapshot.qualityScore());
        data.put("schemaDigestSha256", snapshot.schemaDigestSha256());
        data.put("artifactDigestSha256", snapshot.artifactDigestSha256());
        data.put("lineageReference", snapshot.lineageReference());
        data.put("publishedAt", snapshot.publishedAt().toString());
        data.put("usageBoundary", USAGE_BOUNDARY);
        return data;
    }

    /** 保存从 DataOps PostgreSQL 聚合的不可变数据产品快照。 */
    private record ProductSnapshot(
            String productVersion,
            String status,
            long rowCount,
            long positiveCount,
            long negativeCount,
            long symbolCount,
            LocalDate sourceDateFrom,
            LocalDate sourceDateTo,
            String schemaDigestSha256,
            String artifactDigestSha256,
            String lineageReference,
            double qualityScore,
            OffsetDateTime publishedAt,
            long trainCount,
            long validationCount,
            long testCount) {
    }
}
