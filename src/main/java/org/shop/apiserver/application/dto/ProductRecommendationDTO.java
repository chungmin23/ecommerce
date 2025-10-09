package org.shop.apiserver.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductRecommendationDTO {

    // 사용자 질문
    private String userQuery;

    // 추천된 상품 목록
    private List<ProductDTO> recommendedProducts;

    // AI 추천 설명
    private String explanation;

    // 신뢰도 점수
    private Double confidence;
}
