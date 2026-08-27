package com.synapxnet.dataopstskservice.recommendation;

import com.synapxnet.dataopstskservice.common.Result;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tsk/recommendation-products")
public class RecommendationProductController {

    private final RecommendationProductService recommendationProductService;
    private final RecommendationWriteAuthorizer writeAuthorizer;

    /** 注入推荐数据产品服务。 */
    public RecommendationProductController(
            RecommendationProductService recommendationProductService,
            RecommendationWriteAuthorizer writeAuthorizer
    ) {
        this.recommendationProductService = recommendationProductService;
        this.writeAuthorizer = writeAuthorizer;
    }

    /** 查询全部推荐训练数据产品版本。 */
    @GetMapping
    public Result<List<RecommendationProduct>> listProducts() {
        return Result.success(recommendationProductService.listProducts());
    }

    /** 按版本查询一个推荐训练数据产品。 */
    @GetMapping("/{productVersion}")
    public Result<RecommendationProduct> getProduct(
            @PathVariable("productVersion") String productVersion
    ) {
        return Result.success(recommendationProductService.getProduct(productVersion));
    }

    /** 按审批号和幂等键触发原始数据聚合。 */
    @PostMapping("/build")
    public Result<RecommendationProduct> buildProduct(
            @RequestBody BuildRecommendationProductRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader("X-Approval-Id") String approvalId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        writeAuthorizer.authorize(authorization);
        return Result.success(
                recommendationProductService.buildProduct(request, approvalId, idempotencyKey)
        );
    }

    /** 按审批号和幂等键发布已验证的数据产品。 */
    @PostMapping("/{productVersion}/publish")
    public Result<RecommendationProduct> publishProduct(
            @PathVariable("productVersion") String productVersion,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader("X-Approval-Id") String approvalId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        writeAuthorizer.authorize(authorization);
        return Result.success(
                recommendationProductService.publishProduct(productVersion, approvalId, idempotencyKey)
        );
    }
}
