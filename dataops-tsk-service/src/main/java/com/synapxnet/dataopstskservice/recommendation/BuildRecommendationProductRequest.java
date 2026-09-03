package com.synapxnet.dataopstskservice.recommendation;

/** 保存推荐数据产品构建所需的版本、契约与血缘参数。 */
public record BuildRecommendationProductRequest(
        String productVersion,
        String lineageReference
) {
}
