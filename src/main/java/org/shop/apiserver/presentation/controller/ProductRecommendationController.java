package org.shop.apiserver.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.dto.ProductDTO;
import org.shop.apiserver.application.dto.ProductRecommendationDTO;
import org.shop.apiserver.application.service.ProductRecommendationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Log4j2
public class ProductRecommendationController {

    private final ProductRecommendationService recommendationService;

    /**
     * ⭐ AI 상품 추천 (Redis 캐싱 적용)
     * GET /api/recommendations?query=겨울에 입을 따뜻한 옷 추천해줘
     * 
     * - 첫 요청: 3~5초
     * - 캐시 히트: 0.05초
     */
    @GetMapping("")
    public ProductRecommendationDTO recommend(@RequestParam String query) {

        long startTime = System.currentTimeMillis();
        log.info("추천 요청: {}", query);

        ProductRecommendationDTO result = recommendationService.recommendProducts(query);

        long duration = System.currentTimeMillis() - startTime;
        log.info("추천 응답 완료 ({}ms)", duration);

        return result;
    }

    /**
     * 빠른 벡터 검색 (AI 없이, 캐싱 적용)
     * GET /api/recommendations/search?query=노트북&topK=5
     */
    @GetMapping("/search")
    public List<ProductDTO> searchSimilar(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {

        long startTime = System.currentTimeMillis();
        log.info("벡터 검색 요청: {}", query);

        List<ProductDTO> products = recommendationService.searchSimilarProducts(query, topK);

        long duration = System.currentTimeMillis() - startTime;
        log.info("벡터 검색 응답 ({}ms) - {}개", duration, products.size());

        return products;
    }

    /**
     * 전체 상품 벡터 인덱싱
     * POST /api/recommendations/index/all
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/index/all")
    public Map<String, String> indexAllProducts() {

        long startTime = System.currentTimeMillis();
        log.info("인덱싱 시작");

        recommendationService.indexAllProducts();

        long duration = System.currentTimeMillis() - startTime;
        log.info("인덱싱 완료 ({}ms)", duration);

        return Map.of(
                "result", "SUCCESS",
                "message", "모든 상품이 벡터 DB에 저장되었습니다.",
                "duration", duration + "ms"
        );
    }

    /**
     * 특정 상품만 인덱싱
     * POST /api/recommendations/index/{pno}
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/index/{pno}")
    public Map<String, String> indexProduct(@PathVariable Long pno) {

        log.info("상품 인덱싱: {}", pno);

        recommendationService.indexProduct(pno);

        return Map.of(
                "result", "SUCCESS",
                "message", "상품이 벡터 DB에 저장되고 캐시가 초기화되었습니다."
        );
    }

    /**
     * 캐시 초기화 (상품 변경 시)
     * POST /api/recommendations/cache/clear
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/cache/clear")
    public Map<String, String> clearCache() {

        log.info("캐시 초기화 요청");

        recommendationService.clearCache();

        return Map.of(
                "result", "SUCCESS",
                "message", "캐시가 초기화되었습니다."
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
        long startTime = System.currentTimeMillis();
        log.info("챗봇 요청: {}", message);

        ProductRecommendationDTO result = recommendationService.recommendProducts(message);

        long duration = System.currentTimeMillis() - startTime;
        log.info("ㅗ 챗봇 응답 ({}ms)", duration);

        return Map.of(
                "response", result.getExplanation(),
                "products", result.getRecommendedProducts(),
                "confidence", result.getConfidence(),
                "responseTime", duration + "ms"
        );
    }
}
