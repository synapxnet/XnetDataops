package com.synapxnet.dataopstskservice.recommendation;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class RecommendationProductService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationProductService.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,128}$");
    private static final Pattern AUDIT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{6,128}$");
    private static final String TRAINING_SCHEMA_CONTRACT = "[\"product_version\",\"event_key\",\"user_key\",\"item_key\",\"behavior_type\",\"user_type\",\"user_sex\",\"user_manufacturer_type\",\"user_source\",\"item_type\",\"item_category_key\",\"item_tag_set_key\",\"item_duration_seconds\",\"behavior_duration_ms\",\"read_percent\",\"like_status\",\"label\",\"event_date\",\"dataset_split\"]";

    private final String jdbcUrl;
    private final String username;
    private final String password;

    /** 注入推荐业务 PostgreSQL 连接配置。 */
    public RecommendationProductService(
            @Value("${xnet.recommendation.jdbc-url}") String jdbcUrl,
            @Value("${xnet.recommendation.username}") String username,
            @Value("${xnet.recommendation.password}") String password
    ) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    /** 查询全部推荐训练数据产品版本。 */
    public List<RecommendationProduct> listProducts() {
        String sql = """
                SELECT product_name, product_version, row_count, positive_count, negative_count,
                       schema_digest_sha256, artifact_digest_sha256, lineage_reference, status,
                       created_at, published_at
                FROM recommendation_curated.data_product_registry
                ORDER BY created_at DESC
                """;
        List<RecommendationProduct> products = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                products.add(mapProduct(resultSet));
            }
            return products;
        } catch (SQLException exception) {
            throw new IllegalStateException("查询推荐数据产品失败", exception);
        }
    }

    /** 使用审批和幂等边界从原始层重建推荐训练数据产品。 */
    public RecommendationProduct buildProduct(
            BuildRecommendationProductRequest request,
            String approvalId,
            String idempotencyKey
    ) {
        validateBuildRequest(request, approvalId, idempotencyKey);
        String requestDigest = calculateRequestDigest("build", request, approvalId);
        RecommendationProduct replay = findIdempotentResult(idempotencyKey, "build", requestDigest);
        if (replay != null) {
            return replay;
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureVersionCanBeBuilt(connection, request.productVersion());
                insertExecution(
                        connection,
                        idempotencyKey,
                        "build",
                        request.productVersion(),
                        approvalId,
                        requestDigest
                );
                deleteTrainingVersion(connection, request.productVersion());
                insertTrainingVersion(connection, request.productVersion());
                ProductCounts counts = readProductCounts(connection, request.productVersion());
                validateProductCounts(counts);
                String artifactDigest = calculateArtifactDigest(connection, request.productVersion());
                upsertValidatedProduct(connection, request, counts, artifactDigest);
                completeExecution(connection, idempotencyKey, counts.toSummary());
                connection.commit();
                return getProduct(request.productVersion());
            } catch (Exception exception) {
                connection.rollback();
                recordFailedExecution(
                        idempotencyKey,
                        "build",
                        request.productVersion(),
                        approvalId,
                        requestDigest,
                        exception.getMessage()
                );
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("推荐数据产品构建失败", exception);
        }
    }

    /** 在人工审批后将已验证版本发布给 MLOps。 */
    public RecommendationProduct publishProduct(
            String productVersion,
            String approvalId,
            String idempotencyKey
    ) {
        validateAuditFields(productVersion, approvalId, idempotencyKey);
        BuildRecommendationProductRequest digestRequest = new BuildRecommendationProductRequest(
                productVersion,
                "publish"
        );
        String requestDigest = calculateRequestDigest("publish", digestRequest, approvalId);
        RecommendationProduct replay = findIdempotentResult(idempotencyKey, "publish", requestDigest);
        if (replay != null) {
            return replay;
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                RecommendationProduct product = findProduct(connection, productVersion);
                if (!"validated".equals(product.status())) {
                    throw new IllegalArgumentException("只有 validated 状态的数据产品可以发布");
                }
                insertExecution(
                        connection,
                        idempotencyKey,
                        "publish",
                        productVersion,
                        approvalId,
                        requestDigest
                );
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE recommendation_curated.data_product_registry " +
                                "SET status='published', published_at=CURRENT_TIMESTAMP " +
                                "WHERE product_version=? AND status='validated'"
                )) {
                    statement.setString(1, productVersion);
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalStateException("数据产品发布状态更新失败");
                    }
                }
                completeExecution(connection, idempotencyKey, "status=published");
                connection.commit();
                return getProduct(productVersion);
            } catch (Exception exception) {
                connection.rollback();
                recordFailedExecution(
                        idempotencyKey,
                        "publish",
                        productVersion,
                        approvalId,
                        requestDigest,
                        exception.getMessage()
                );
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("推荐数据产品发布失败", exception);
        }
    }

    /** 打开推荐业务 PostgreSQL 连接并拒绝缺失的运行时凭据。 */
    private Connection openConnection() throws SQLException {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("RECOMMENDATION_DATAOPS_DB_PASSWORD 未配置");
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /** 校验构建请求的版本、血缘和审计字段。 */
    private void validateBuildRequest(
            BuildRecommendationProductRequest request,
            String approvalId,
            String idempotencyKey
    ) {
        if (request == null) {
            throw new IllegalArgumentException("构建请求不能为空");
        }
        validateAuditFields(request.productVersion(), approvalId, idempotencyKey);
        if (request.lineageReference() == null
                || request.lineageReference().isBlank()
                || request.lineageReference().length() > 256) {
            throw new IllegalArgumentException("血缘引用不能为空且不能超过 256 个字符");
        }
    }

    /** 校验版本、审批号和幂等键是否满足受控操作格式。 */
    private void validateAuditFields(String productVersion, String approvalId, String idempotencyKey) {
        if (!VERSION_PATTERN.matcher(normalize(productVersion)).matches()) {
            throw new IllegalArgumentException("数据产品版本格式不合法");
        }
        if (!AUDIT_ID_PATTERN.matcher(normalize(approvalId)).matches()) {
            throw new IllegalArgumentException("X-Approval-Id 缺失或格式不合法");
        }
        if (!AUDIT_ID_PATTERN.matcher(normalize(idempotencyKey)).matches()) {
            throw new IllegalArgumentException("Idempotency-Key 缺失或格式不合法");
        }
    }

    /** 将可空文本规范为空字符串，供格式校验使用。 */
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /** 计算操作、参数和审批号的规范请求摘要。 */
    private String calculateRequestDigest(
            String action,
            BuildRecommendationProductRequest request,
            String approvalId
    ) {
        String canonical = String.join(
                "|",
                action,
                normalize(request.productVersion()),
                normalize(request.lineageReference()),
                normalize(approvalId)
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算请求摘要", exception);
        }
    }

    /** 查询成功的幂等执行记录并验证请求摘要未发生冲突。 */
    private RecommendationProduct findIdempotentResult(
            String idempotencyKey,
            String action,
            String requestDigest
    ) {
        String sql = """
                SELECT action, product_version, request_digest_sha256, status
                FROM recommendation_curated.data_product_execution
                WHERE idempotency_key=?
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idempotencyKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                if (!action.equals(resultSet.getString("action"))
                        || !requestDigest.equals(resultSet.getString("request_digest_sha256"))) {
                    throw new IllegalArgumentException("幂等键已被不同请求使用");
                }
                if ("succeeded".equals(resultSet.getString("status"))) {
                    return getProduct(resultSet.getString("product_version"));
                }
                throw new IllegalStateException("相同幂等请求仍在执行或此前失败");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("读取幂等执行记录失败", exception);
        }
    }

    /** 阻止已发布版本被重新构建覆盖。 */
    private void ensureVersionCanBeBuilt(Connection connection, String productVersion) throws SQLException {
        String sql = "SELECT status FROM recommendation_curated.data_product_registry WHERE product_version=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && "published".equals(resultSet.getString("status"))) {
                    throw new IllegalArgumentException("已发布数据产品不可覆盖，请使用新版本号");
                }
            }
        }
    }

    /** 写入运行中的数据产品操作审计记录。 */
    private void insertExecution(
            Connection connection,
            String idempotencyKey,
            String action,
            String productVersion,
            String approvalId,
            String requestDigest
    ) throws SQLException {
        String sql = """
                INSERT INTO recommendation_curated.data_product_execution
                    (idempotency_key, action, product_version, approval_id, request_digest_sha256, status)
                VALUES (?, ?, ?, ?, ?, 'running')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idempotencyKey);
            statement.setString(2, action);
            statement.setString(3, productVersion);
            statement.setString(4, approvalId);
            statement.setString(5, requestDigest);
            statement.executeUpdate();
        }
    }

    /** 删除同版本未发布的旧训练记录。 */
    private void deleteTrainingVersion(Connection connection, String productVersion) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM recommendation_curated.dcn_training WHERE product_version=?"
        )) {
            statement.setString(1, productVersion);
            statement.executeUpdate();
        }
    }

    /** 从原始层关联用户、内容和行为并写入版本化训练表。 */
    private void insertTrainingVersion(Connection connection, String productVersion) throws SQLException {
        String sql = """
                INSERT INTO recommendation_curated.dcn_training (
                    product_version, event_key, user_key, item_key, behavior_type, user_type,
                    user_sex, user_manufacturer_type, user_source, item_type, item_category_key,
                    item_tag_set_key, item_duration_seconds, behavior_duration_ms, read_percent,
                    like_status, label, event_date, dataset_split
                )
                SELECT ?, interaction.event_key, interaction.user_key, interaction.item_key,
                       interaction.behavior_type, user_profile.user_type, user_profile.sex,
                       user_profile.manufacturer_type, user_profile.source, item.item_type,
                       item.category_key, item.tag_set_key, item.duration_seconds,
                       interaction.duration_ms, interaction.read_percent, interaction.like_status,
                       interaction.label, interaction.event_date,
                       CASE
                           WHEN get_byte(decode(md5(interaction.event_key), 'hex'), 0) < 205 THEN 'train'
                           WHEN get_byte(decode(md5(interaction.event_key), 'hex'), 0) < 230 THEN 'validation'
                           ELSE 'test'
                       END
                FROM recommendation_raw.interactions AS interaction
                INNER JOIN recommendation_raw.users AS user_profile
                    ON user_profile.user_key = interaction.user_key
                INNER JOIN recommendation_raw.items AS item
                    ON item.item_key = interaction.item_key
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productVersion);
            statement.executeUpdate();
        }
    }

    /** 统计数据产品总量、标签分布和集合划分。 */
    private ProductCounts readProductCounts(Connection connection, String productVersion) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS row_count,
                       COUNT(*) FILTER (WHERE label=1) AS positive_count,
                       COUNT(*) FILTER (WHERE label=0) AS negative_count,
                       COUNT(*) FILTER (WHERE dataset_split='train') AS train_count,
                       COUNT(*) FILTER (WHERE dataset_split='validation') AS validation_count,
                       COUNT(*) FILTER (WHERE dataset_split='test') AS test_count
                FROM recommendation_curated.dcn_training
                WHERE product_version=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new ProductCounts(
                        resultSet.getLong("row_count"),
                        resultSet.getLong("positive_count"),
                        resultSet.getLong("negative_count"),
                        resultSet.getLong("train_count"),
                        resultSet.getLong("validation_count"),
                        resultSet.getLong("test_count")
                );
            }
        }
    }

    /** 确认数据量、双标签和三个训练集合均非空。 */
    private void validateProductCounts(ProductCounts counts) {
        if (counts.rowCount() < 1_000) {
            throw new IllegalStateException("训练数据产品少于 1000 条，不允许发布");
        }
        if (counts.positiveCount() == 0 || counts.negativeCount() == 0) {
            throw new IllegalStateException("训练数据产品必须同时包含正负样本");
        }
        if (counts.trainCount() == 0 || counts.validationCount() == 0 || counts.testCount() == 0) {
            throw new IllegalStateException("训练、验证和测试集合均不能为空");
        }
    }

    /** 登记或更新已通过质量检查的数据产品版本。 */
    private void upsertValidatedProduct(
            Connection connection,
            BuildRecommendationProductRequest request,
            ProductCounts counts,
            String artifactDigest
    ) throws SQLException {
        String sql = """
                INSERT INTO recommendation_curated.data_product_registry (
                    product_name, product_version, row_count, positive_count, negative_count,
                    schema_digest_sha256, artifact_digest_sha256, lineage_reference, status
                ) VALUES ('recommendation_dcn_training', ?, ?, ?, ?, ?, ?, ?, 'validated')
                ON CONFLICT (product_version) DO UPDATE SET
                    row_count=EXCLUDED.row_count,
                    positive_count=EXCLUDED.positive_count,
                    negative_count=EXCLUDED.negative_count,
                    schema_digest_sha256=EXCLUDED.schema_digest_sha256,
                    artifact_digest_sha256=EXCLUDED.artifact_digest_sha256,
                    lineage_reference=EXCLUDED.lineage_reference,
                    status='validated',
                    published_at=NULL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.productVersion());
            statement.setLong(2, counts.rowCount());
            statement.setLong(3, counts.positiveCount());
            statement.setLong(4, counts.negativeCount());
            statement.setString(5, calculateSha256(TRAINING_SCHEMA_CONTRACT));
            statement.setString(6, artifactDigest);
            statement.setString(7, request.lineageReference());
            statement.executeUpdate();
        }
    }

    /** 按稳定顺序计算真实训练记录的内容摘要。 */
    private String calculateArtifactDigest(Connection connection, String productVersion) throws SQLException {
        String sql = """
                SELECT event_key, label, dataset_split
                FROM recommendation_curated.dcn_training
                WHERE product_version=?
                ORDER BY event_key
                """;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, productVersion);
                statement.setFetchSize(1_000);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String canonicalRow = String.join(
                                "|",
                                resultSet.getString("event_key"),
                                String.valueOf(resultSet.getInt("label")),
                                resultSet.getString("dataset_split")
                        ) + "\n";
                        digest.update(canonicalRow.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (SQLException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算训练数据产品摘要", exception);
        }
    }

    /** 计算 UTF-8 文本的 SHA-256 十六进制摘要。 */
    private String calculateSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算 Schema 摘要", exception);
        }
    }

    /** 将操作审计记录更新为成功并保存受限长度摘要。 */
    private void completeExecution(Connection connection, String idempotencyKey, String summary)
            throws SQLException {
        String sql = """
                UPDATE recommendation_curated.data_product_execution
                SET status='succeeded', result_summary=?, finished_at=CURRENT_TIMESTAMP
                WHERE idempotency_key=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, truncate(summary, 512));
            statement.setString(2, idempotencyKey);
            statement.executeUpdate();
        }
    }

    /** 在主事务回滚后以独立事务保存失败审计记录。 */
    private void recordFailedExecution(
            String idempotencyKey,
            String action,
            String productVersion,
            String approvalId,
            String requestDigest,
            String errorSummary
    ) {
        String sql = """
                INSERT INTO recommendation_curated.data_product_execution (
                    idempotency_key, action, product_version, approval_id, request_digest_sha256,
                    status, error_summary, finished_at
                ) VALUES (?, ?, ?, ?, ?, 'failed', ?, CURRENT_TIMESTAMP)
                ON CONFLICT (idempotency_key) DO NOTHING
                """;
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, idempotencyKey);
            statement.setString(2, action);
            statement.setString(3, productVersion);
            statement.setString(4, approvalId);
            statement.setString(5, requestDigest);
            statement.setString(6, truncate(errorSummary, 512));
            statement.executeUpdate();
        } catch (Exception auditException) {
            log.error(
                    "Failed to persist recommendation product failure audit: action={}, version={}, idempotencyKey={}",
                    action,
                    productVersion,
                    idempotencyKey,
                    auditException
            );
        }
    }

    /** 按版本查询单个数据产品。 */
    public RecommendationProduct getProduct(String productVersion) {
        try (Connection connection = openConnection()) {
            return findProduct(connection, productVersion);
        } catch (SQLException exception) {
            throw new IllegalStateException("查询数据产品版本失败", exception);
        }
    }

    /** 在指定连接中按版本查询数据产品。 */
    private RecommendationProduct findProduct(Connection connection, String productVersion) throws SQLException {
        String sql = """
                SELECT product_name, product_version, row_count, positive_count, negative_count,
                       schema_digest_sha256, artifact_digest_sha256, lineage_reference, status,
                       created_at, published_at
                FROM recommendation_curated.data_product_registry
                WHERE product_version=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("数据产品版本不存在: " + productVersion);
                }
                return mapProduct(resultSet);
            }
        }
    }

    /** 将 JDBC 查询结果转换为数据产品对象。 */
    private RecommendationProduct mapProduct(ResultSet resultSet) throws SQLException {
        Timestamp publishedAt = resultSet.getTimestamp("published_at");
        return new RecommendationProduct(
                resultSet.getString("product_name"),
                resultSet.getString("product_version"),
                resultSet.getLong("row_count"),
                resultSet.getLong("positive_count"),
                resultSet.getLong("negative_count"),
                resultSet.getString("schema_digest_sha256"),
                resultSet.getString("artifact_digest_sha256"),
                resultSet.getString("lineage_reference"),
                resultSet.getString("status"),
                resultSet.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
                publishedAt == null ? null : publishedAt.toInstant().atOffset(ZoneOffset.UTC)
        );
    }

    /** 截断审计摘要，避免异常堆栈或上游内容无限写入数据库。 */
    private String truncate(String value, int maxLength) {
        String safeValue = value == null ? "unknown" : value;
        return safeValue.length() <= maxLength ? safeValue : safeValue.substring(0, maxLength);
    }

    /** 保存数据产品质量统计。 */
    private record ProductCounts(
            long rowCount,
            long positiveCount,
            long negativeCount,
            long trainCount,
            long validationCount,
            long testCount
    ) {
        /** 生成供审计回执展示的固定字段摘要。 */
        private String toSummary() {
            return String.format(
                    "rows=%d,positive=%d,negative=%d,train=%d,validation=%d,test=%d",
                    rowCount,
                    positiveCount,
                    negativeCount,
                    trainCount,
                    validationCount,
                    testCount
            );
        }
    }
}
