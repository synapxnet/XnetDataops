package com.synapxnet.dataopstskservice.recommendation;

import java.time.OffsetDateTime;

/** 表示 DataOps 发布给 MLOps 的推荐训练数据产品。 */
public record RecommendationProduct(
        String productName,
        String productVersion,
        long rowCount,
        long positiveCount,
        long negativeCount,
        String schemaDigestSha256,
        String artifactDigestSha256,
        String lineageReference,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt
) {
}
