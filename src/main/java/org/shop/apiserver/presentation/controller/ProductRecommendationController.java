package org.shop.apiserver.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.dto.ProductDTO;
import org.shop.apiserver.application.dto.ProductRecommendationDTO;
import org.shop.apiserver.application.service.ProductRecommendationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Log4j2
public class ProductRecommendationController {

    private final ProductRecommendationService recommendationService;

    /**
     * AI 상품 추천 (RAG 사용)
     * GET /api/recommendations?query=겨울에 입을 따뜻한 옷 추천해줘
     */
    @GetMapping("")
    public ProductRecommendationDTO recommend(@RequestParam String query) {

        log.info("상품 추천 요청: " + query);

        return recommendationService.recommendProducts(query);
    }

    /**
     * 벡터 검색 (AI 없이 유사 상품만 검색)
     * GET /api/recommendations/search?query=노트북&topK=5
     */
    @GetMapping("/search")
    public List<ProductDTO> searchSimilar(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {

        log.info("벡터 검색: " + query);

        return recommendationService.searchSimilarProducts(query, topK);
    }

    /**
     * 전체 상품 벡터 인덱싱 (초기 설정 시 한 번 실행)
     * POST /api/recommendations/index/all
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/index/all")
    public Map<String, String> indexAllProducts() {

        log.info("전체 상품 인덱싱 시작");

        recommendationService.indexAllProducts();

        return Map.of(
                "result", "SUCCESS",
                "message", "모든 상품이 벡터 DB에 저장되었습니다."
        );
    }

    /**
     * 특정 상품만 인덱싱
     * POST /api/recommendations/index/{pno}
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/index/{pno}")
    public Map<String, String> indexProduct(@PathVariable Long pno) {

        log.info("상품 인덱싱: " + pno);

        recommendationService.indexProduct(pno);

        return Map.of(
                "result", "SUCCESS",
                "message", "상품이 벡터 DB에 저장되었습니다."
        );
    }

    /**
     * 챗봇 스타일 대화
     * POST /api/recommendations/chat
     * Body: { "message": "20대 여성 선물 추천" }
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {

        String message = request.get("message");
        log.info("챗봇 메시지: " + message);

        ProductRecommendationDTO result = recommendationService.recommendProducts(message);

        return Map.of(
                "response", result.getExplanation(),
                "products", result.getRecommendedProducts(),
                "confidence", result.getConfidence()
        );
    }
}