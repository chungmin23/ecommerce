package org.shop.apiserver.service;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.shop.apiserver.application.dto.ProductDTO;
import org.shop.apiserver.application.dto.ProductRecommendationDTO;
import org.shop.apiserver.application.service.ProductRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Log4j2
public class ProductRecommendationServiceTests {

    @Autowired
    private ProductRecommendationService recommendationService;

    /**
     * 1단계: 먼저 상품 인덱싱 (한 번만 실행)
     */
    @Test
    public void step1_indexProducts() {

        log.info("=== 상품 인덱싱 테스트 ===");

        recommendationService.indexAllProducts();

        log.info("인덱싱 완료");
    }

    /**
     * 2단계: RAG 추천 테스트
     */
    @Test
    public void step2_testRecommendation() {

        log.info("=== RAG 추천 테스트 ===");

        String query = "1만원 이하의 저렴한 상품 추천해줘";

        ProductRecommendationDTO result = recommendationService.recommendProducts(query);

        log.info("사용자 질문: " + result.getUserQuery());
        log.info("신뢰도: " + result.getConfidence());
        log.info("\n=== AI 설명 ===\n" + result.getExplanation());
        log.info("\n=== 추천 상품 ===");

        result.getRecommendedProducts().forEach(product -> {
            log.info(String.format("- [%d] %s : %,d원",
                    product.getPno(),
                    product.getPname(),
                    product.getPrice()
            ));
        });
    }

    /**
     * 3단계: 벡터 검색만 테스트
     */
    @Test
    public void step3_testVectorSearch() {

        log.info("=== 벡터 검색 테스트 ===");

        List<ProductDTO> products = recommendationService.searchSimilarProducts("노트북", 5);

        log.info("검색 결과: " + products.size() + "개");

        products.forEach(product -> {
            log.info(String.format("- %s (%,d원)",
                    product.getPname(),
                    product.getPrice()
            ));
        });
    }

    /**
     * 다양한 질문 테스트
     */
    @Test
    public void step4_testVariousQueries() {

        String[] queries = {
                "겨울에 입을 따뜻한 옷 추천",
                "20대 여성 선물 추천",
                "가성비 좋은 상품",
                "고급스러운 상품",
                "5만원 이하 상품"
        };

        for (String query : queries) {
            log.info("\n========================================");
            log.info("질문: " + query);
            log.info("========================================");

            ProductRecommendationDTO result = recommendationService.recommendProducts(query);

            log.info("추천 상품 수: " + result.getRecommendedProducts().size());
            log.info("AI 답변:\n" + result.getExplanation());
        }
    }
}