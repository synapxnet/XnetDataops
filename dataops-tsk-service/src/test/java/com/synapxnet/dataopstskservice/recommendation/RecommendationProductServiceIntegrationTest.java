package com.synapxnet.dataopstskservice.recommendation;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RecommendationProductServiceIntegrationTest {

    /** 验证真实 PostgreSQL 上的数据产品构建、幂等重放和审批发布闭环。 */
    @Test
    void buildsAndPublishesRealRecommendationProduct() {
        String password = System.getenv("RECOMMENDATION_TEST_DB_PASSWORD");
        assumeTrue(password != null && !password.isBlank(), "未配置本地 PostgreSQL 集成测试密码");
        RecommendationProductService service = new RecommendationProductService(
                "jdbc:postgresql://127.0.0.1:55432/recommendation",
                "postgres",
                password
        );
        BuildRecommendationProductRequest request = new BuildRecommendationProductRequest(
                "recommendation-dcn-integration-v2",
                "dataops://recommendation/raw/integration-v2"
        );

        RecommendationProduct built = service.buildProduct(
                request,
                "APR-INTEGRATION-001",
                "IDEM-BUILD-INTEGRATION-002"
        );
        RecommendationProduct replayed = service.buildProduct(
                request,
                "APR-INTEGRATION-001",
                "IDEM-BUILD-INTEGRATION-002"
        );
        RecommendationProduct published = service.publishProduct(
                request.productVersion(),
                "APR-INTEGRATION-002",
                "IDEM-PUBLISH-INTEGRATION-002"
        );

        assertTrue(Set.of("validated", "published").contains(built.status()));
        assertEquals(built.productVersion(), replayed.productVersion());
        assertEquals("published", published.status());
        assertEquals(80_000, published.rowCount());
        assertEquals(40_000, published.positiveCount());
        assertEquals(40_000, published.negativeCount());
        assertTrue(service.listProducts().stream()
                .anyMatch(product -> request.productVersion().equals(product.productVersion())));
    }
}
