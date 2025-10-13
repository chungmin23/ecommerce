package org.shop.apiserver.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.dto.ProductDTO;
import org.shop.apiserver.application.dto.ProductRecommendationDTO;
import org.shop.apiserver.domain.model.product.Product;
import org.shop.apiserver.infrastructure.persistence.jpa.ProductRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ⭐ Redis 캐싱 기반 상품 추천 서비스
 * - 동일한 질문에 대해 캐시된 결과 반환 (1시간 TTL)
 * - 첫 요청: 3~5초, 이후 요청: 0.05초
 */
@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class ProductRecommendationService {

    private final ChatClient.Builder chatClientBuilder;
    private final ProductRepository productRepository;
    private final VectorStore vectorStore;

    /**
     * ⭐ RAG 기반 상품 추천 (캐싱 적용)
     * 1. RETRIEVAL: 벡터 검색
     * 2. AUGMENTED: 컨텍스트 구성
     * 3. GENERATION: AI 답변 생성
     */
    @Cacheable(value = "product-recommendations", key = "#userQuery")
    public ProductRecommendationDTO recommendProducts(String userQuery) {

        long startTime = System.currentTimeMillis();
        log.info(" RAG 상품 추천 시작: {}", userQuery);

        try {
            // 1. RETRIEVAL: 벡터 검색
            List<Document> similarDocs = vectorStore.similaritySearch(
                    SearchRequest.query(userQuery)
                            .withTopK(5)
                            .withSimilarityThreshold(0.6)
            );

            if (similarDocs.isEmpty()) {
                log.warn("⚠유사한 상품을 찾지 못했습니다.");
                return createEmptyRecommendation(userQuery);
            }

            // 2. AUGMENTED: 컨텍스트 구성
            String context = similarDocs.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("\n\n"));

            // 3. GENERATION: AI 답변 생성
            String prompt = createPrompt(userQuery, context);

            ChatClient chatClient = chatClientBuilder.build();
            String aiResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 4. 응답 파싱 및 상품 조회
            List<ProductDTO> recommendedProducts = parseAndFetchProducts(aiResponse);

            long duration = System.currentTimeMillis() - startTime;
            log.info("RAG 추천 완료 ({}ms)", duration);

            return ProductRecommendationDTO.builder()
                    .userQuery(userQuery)
                    .recommendedProducts(recommendedProducts)
                    .explanation(aiResponse)
                    .confidence(0.85)
                    .build();

        } catch (Exception e) {
            log.error("RAG 추천 실패: {}", e.getMessage(), e);
            return createEmptyRecommendation(userQuery);
        }
    }

    /**
     * 빠른 벡터 검색 (AI 없이, 캐싱 적용)
     */
    @Cacheable(value = "vector-search", key = "#query + '-' + #topK")
    public List<ProductDTO> searchSimilarProducts(String query, int topK) {

        long startTime = System.currentTimeMillis();
        log.info("벡터 검색 시작: {}", query);

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.query(query)
                        .withTopK(topK)
                        .withSimilarityThreshold(0.5)
        );

        List<ProductDTO> products = results.stream()
                .map(doc -> {
                    Long pno = ((Number) doc.getMetadata().get("pno")).longValue();
                    return productRepository.findById(pno)
                            .map(this::convertToDTO)
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        log.info("벡터 검색 완료 ({}ms) - {}개 상품", duration, products.size());

        return products;
    }

    /**
     * 캐시 초기화 (상품 변경 시 호출)
     */
    @CacheEvict(value = {"product-recommendations", "vector-search"}, allEntries = true)
    public void clearCache() {
        log.info("캐시 초기화 완료");
    }

    /**
     * 모든 상품을 벡터 DB에 인덱싱
     */
    public void indexAllProducts() {

        log.info("상품 벡터 인덱싱 시작");

        List<Product> products = productRepository.findAll();
        List<Document> documents = new ArrayList<>();

        for (Product product : products) {
            if (product.isDelFlag()) continue;

            String content = formatProductContent(product);
            Map<String, Object> metadata = createMetadata(product);

            Document doc = new Document(
                    "product_" + product.getPno(),
                    content,
                    metadata
            );

            documents.add(doc);
        }

        vectorStore.add(documents);

        log.info("인덱싱 완료: {}개 상품", documents.size());
    }

    /**
     * 특정 상품만 인덱싱
     */
    public void indexProduct(Long pno) {

        Product product = productRepository.findById(pno)
                .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다."));

        String content = formatProductContent(product);
        Map<String, Object> metadata = createMetadata(product);

        Document doc = new Document(
                "product_" + product.getPno(),
                content,
                metadata
        );

        vectorStore.add(List.of(doc));
        
        // 캐시 초기화
        clearCache();
        
        log.info("상품 인덱싱 및 캐시 초기화 완료: {}", product.getPname());
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    private String formatProductContent(Product product) {
        return String.format(
                "상품번호: %d\n상품명: %s\n가격: %,d원\n설명: %s",
                product.getPno(),
                product.getPname(),
                product.getPrice(),
                product.getPdesc() != null ? product.getPdesc() : "설명 없음"
        );
    }

    private Map<String, Object> createMetadata(Product product) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pno", product.getPno());
        metadata.put("pname", product.getPname());
        metadata.put("price", product.getPrice());
        metadata.put("pdesc", product.getPdesc());
        return metadata;
    }

    private String createPrompt(String userQuery, String context) {
        return String.format("""
            당신은 쇼핑몰의 AI 상품 추천 어시스턴트입니다.
            
            사용자 질문: "%s"
            
            관련 상품 정보:
            %s
            
            위 상품들을 분석하여 사용자에게 가장 적합한 상품을 추천하고 이유를 설명해주세요.
            
            응답 형식:
            추천 상품 번호: [번호1, 번호2, 번호3]
            추천 이유: [상세한 설명을 친절하게 작성]
            """, userQuery, context);
    }

    private List<ProductDTO> parseAndFetchProducts(String aiResponse) {
        List<ProductDTO> result = new ArrayList<>();

        try {
            String[] lines = aiResponse.split("\n");
            for (String line : lines) {
                if (line.contains("추천 상품 번호:") || line.contains("상품 번호:")) {
                    String numbers = line.replaceAll("[^0-9,]", "");
                    String[] pnoArray = numbers.split(",");

                    for (String pnoStr : pnoArray) {
                        if (!pnoStr.trim().isEmpty()) {
                            try {
                                Long pno = Long.parseLong(pnoStr.trim());
                                Optional<Product> productOpt = productRepository.findById(pno);
                                if (productOpt.isPresent()) {
                                    result.add(convertToDTO(productOpt.get()));
                                }
                            } catch (NumberFormatException e) {
                                log.warn("⚠상품 번호 파싱 실패: {}", pnoStr);
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.error("응답 파싱 오류: {}", e.getMessage());
        }

        if (result.isEmpty()) {
            return searchSimilarProducts(aiResponse, 3);
        }

        return result;
    }

    private ProductRecommendationDTO createEmptyRecommendation(String userQuery) {
        return ProductRecommendationDTO.builder()
                .userQuery(userQuery)
                .recommendedProducts(new ArrayList<>())
                .explanation("죄송합니다. 요청하신 조건에 맞는 상품을 찾지 못했습니다.")
                .confidence(0.0)
                .build();
    }

    private ProductDTO convertToDTO(Product product) {
        List<String> imageFiles = product.getImageList().stream()
                .map(img -> img.getFileName())
                .collect(Collectors.toList());

        return ProductDTO.builder()
                .pno(product.getPno())
                .pname(product.getPname())
                .price(product.getPrice())
                .pdesc(product.getPdesc())
                .delFlag(product.isDelFlag())
                .uploadFileNames(imageFiles)
                .build();
    }
}
