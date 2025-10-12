package org.shop.apiserver.repository;// src/test/java/org/shop/apiserver/service/LockPerformanceComparisonTests.java

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.shop.apiserver.application.dto.DeliveryDTO;
import org.shop.apiserver.application.dto.OrderDTO;
import org.shop.apiserver.application.dto.OrderItemDTO;
import org.shop.apiserver.application.service.OrderService;
import org.shop.apiserver.domain.model.product.Product;
import org.shop.apiserver.infrastructure.persistence.jpa.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@Log4j2
public class LockPerformanceComparisonTests {

    @Autowired
    @Qualifier("pessimisticLockOrderService")
    private OrderService pessimisticLockOrderService;

    @Autowired
    @Qualifier("redisLockOrderService")
    private OrderService redisLockOrderService;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 성능 비교: 비관적 락 vs Redis 분산락
     */
    @Test
    @DisplayName("성능 비교 - 비관적 락 vs Redis 분산락")
    public void testPerformanceComparison() throws InterruptedException {

        log.info("\n");
        log.info("========================================");
        log.info("       성능 비교 테스트");
        log.info("========================================");
        log.info("");

        // 1. 비관적 락 테스트
        long pessimisticDuration = runPerformanceTest(
                pessimisticLockOrderService,
                "비관적 락",
                1L,
                50,
                2
        );

        Thread.sleep(3000);  // 쿠링 타임

        // 2. Redis 분산락 테스트 (동일 조건)
        long redisDuration = runPerformanceTest(
                redisLockOrderService,
                "Redis 분산락",
                2L,
                50,
                2
        );

        // 3. 비교 결과
        log.info("\n");
        log.info("========================================");
        log.info("       최종 비교 결과");
        log.info("========================================");
        log.info("비관적 락 실행 시간: {}ms", pessimisticDuration);
        log.info("Redis 분산락 실행 시간: {}ms", redisDuration);
        log.info("성능 개선: {:.2f}배 빠름", (double) pessimisticDuration / redisDuration);
        log.info("========================================\n");
    }

    /**
     * 성능 테스트 실행
     */
    private long runPerformanceTest(
            OrderService orderService,
            String lockType,
            Long productId,
            int threadCount,
            int orderQty) throws InterruptedException {

        // 재고 초기화
        Product product = productRepository.findById(productId).orElseThrow();
        product.changeStock(100);
        productRepository.save(product);

        log.info("\n========== {} 성능 테스트 ==========", lockType);
        log.info("상품 ID: {}", productId);
        log.info("동시 사용자: {}명", threadCount);
        log.info("주문 수량: {}개", orderQty);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // 동시 주문 실행
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    OrderDTO orderDTO = OrderDTO.builder()
                            .email("user1@aaa.com")
                            .orderItems(List.of(
                                    OrderItemDTO.builder()
                                            .pno(productId)
                                            .qty(orderQty)
                                            .build()
                            ))
                            .delivery(DeliveryDTO.builder()
                                    .receiverName("테스터" + index)
                                    .receiverPhone("010-0000-0000")
                                    .address("서울시")
                                    .zipCode("12345")
                                    .build())
                            .paymentMethod("CARD")
                            .build();

                    orderService.createOrder(orderDTO);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 결과 출력
        Product afterProduct = productRepository.findById(productId).orElseThrow();

        log.info("------------------------------------------");
        log.info("실행 시간: {}ms", duration);
        log.info("평균 응답시간: {}ms", duration / threadCount);
        log.info("처리량: {:.2f} req/sec", (threadCount * 1000.0) / duration);
        log.info("성공: {}", successCount.get());
        log.info("실패: {}", failCount.get());
        log.info("최종 재고: {}", afterProduct.getStock());
        log.info("=========================================\n");

        return duration;
    }
}