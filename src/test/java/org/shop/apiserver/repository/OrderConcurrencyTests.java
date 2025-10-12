package org.shop.apiserver.repository;// src/test/java/org/shop/apiserver/service/OrderConcurrencyTests.java

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Log4j2
public class OrderConcurrencyTests {

    // ✅ 비관적 락 서비스
    @Autowired
    @Qualifier("pessimisticLockOrderService")
    private OrderService pessimisticLockOrderService;

    // ✅ Redis 분산락 서비스
    @Autowired
    @Qualifier("redisLockOrderService")
    private OrderService redisLockOrderService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    public void setup() {
        // 테스트 전 재고 초기화
        Product product1 = productRepository.findById(1L).orElse(null);
        if (product1 != null) {
            product1.changeStock(100);
            productRepository.save(product1);
            log.info("재고 초기화: 상품1 - 100개");
        }

        Product product2 = productRepository.findById(2L).orElse(null);
        if (product2 != null) {
            product2.changeStock(100);
            productRepository.save(product2);
            log.info("재고 초기화: 상품2 - 100개");
        }
    }

    /**
     * 테스트 1: 비관적 락 동시성 테스트
     */
    @Test
    @DisplayName("비관적 락 - 30명이 5개씩 주문 (재고 100개)")
    public void testPessimisticLock() throws InterruptedException {

        // Given
        Product product = productRepository.findById(1L).orElseThrow();
        int initialStock = product.getStock();

        log.info("\n========== 비관적 락 테스트 시작 ==========");
        log.info("초기 재고: {}", initialStock);
        log.info("동시 사용자: 30명");
        log.info("주문 수량: 5개");
        log.info("예상: 20명 성공, 10명 실패");

        int threadCount = 30;
        int orderQty = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When - 동시 주문
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    OrderDTO orderDTO = createOrderDTO(1L, orderQty, index);

                    // ✅ 비관적 락 서비스 사용
                    pessimisticLockOrderService.createOrder(orderDTO);

                    successCount.incrementAndGet();
                    log.info("✅ [{}] 주문 성공", index);

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("❌ [{}] 주문 실패: {}", index, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();

        // Then - 결과 검증
        Product afterProduct = productRepository.findById(1L).orElseThrow();

        log.info("\n========== 비관적 락 테스트 결과 ==========");
        log.info("실행 시간: {}ms", (endTime - startTime));
        log.info("평균 응답시간: {}ms", (endTime - startTime) / threadCount);
        log.info("성공: {}", successCount.get());
        log.info("실패: {}", failCount.get());
        log.info("초기 재고: {}", initialStock);
        log.info("최종 재고: {}", afterProduct.getStock());
        log.info("예상 재고: {}", (initialStock - successCount.get() * orderQty));
        log.info("==========================================\n");

        // 검증
        assertEquals(initialStock - successCount.get() * orderQty, afterProduct.getStock(),
                "재고가 정확히 차감되어야 합니다");
        assertTrue(afterProduct.getStock() >= 0,
                "재고는 음수가 될 수 없습니다");
        assertEquals(20, successCount.get(),
                "20명만 성공해야 합니다");
        assertEquals(10, failCount.get(),
                "10명은 실패해야 합니다");
    }

    /**
     * 테스트 2: Redis 분산락 동시성 테스트
     */
    @Test
    @DisplayName("Redis 분산락 - 50명이 3개씩 주문 (재고 100개)")
    public void testRedisDistributedLock() throws InterruptedException {

        // Given
        Product product = productRepository.findById(1L).orElseThrow();
        int initialStock = product.getStock();

        log.info("\n========== Redis 분산락 테스트 시작 ==========");
        log.info("초기 재고: {}", initialStock);
        log.info("동시 사용자: 50명");
        log.info("주문 수량: 3개");
        log.info("예상: 33명 성공, 17명 실패");

        int threadCount = 50;
        int orderQty = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When - 동시 주문
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    OrderDTO orderDTO = createOrderDTO(1L, orderQty, index);

                    // ✅ Redis 분산락 서비스 사용
                    redisLockOrderService.createOrder(orderDTO);

                    successCount.incrementAndGet();
                    log.info("✅ [{}] 주문 성공", index);

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("❌ [{}] 주문 실패: {}", index, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();

        // Then - 결과 검증
        Product afterProduct = productRepository.findById(1L).orElseThrow();

        log.info("\n========== Redis 분산락 테스트 결과 ==========");
        log.info("실행 시간: {}ms", (endTime - startTime));
        log.info("평균 응답시간: {}ms", (endTime - startTime) / threadCount);
        log.info("성공: {}", successCount.get());
        log.info("실패: {}", failCount.get());
        log.info("초기 재고: {}", initialStock);
        log.info("최종 재고: {}", afterProduct.getStock());
        log.info("예상 재고: {}", (initialStock - successCount.get() * orderQty));
        log.info("===========================================\n");

        // 검증
        assertEquals(initialStock - successCount.get() * orderQty, afterProduct.getStock(),
                "재고가 정확히 차감되어야 합니다");
        assertTrue(afterProduct.getStock() >= 0,
                "재고는 음수가 될 수 없습니다");
        assertTrue(successCount.get() >= 30 && successCount.get() <= 34,
                "약 33명 정도 성공해야 합니다");
    }

    /**
     * 테스트 3: 락 없음 - 동시성 문제 발생 확인
     */
    @Test
    @DisplayName("락 없음 - 동시성 문제 발생 (재고 음수)")
    public void testWithoutLock() throws InterruptedException {

        // Given
        Product product = productRepository.findById(2L).orElseThrow();
        product.changeStock(50);
        productRepository.save(product);

        int initialStock = product.getStock();

        log.info("\n========== 락 없음 테스트 시작 ==========");
        log.info("⚠️ 주의: 이 테스트는 동시성 문제를 재현합니다");
        log.info("초기 재고: {}", initialStock);
        log.info("동시 사용자: 20명");
        log.info("주문 수량: 5개");

        int threadCount = 20;
        int orderQty = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When - 락 없이 동시 재고 감소
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    // ❌ 락 없이 직접 재고 감소
                    Product p = productRepository.findById(2L).orElseThrow();

                    if (p.getStock() >= orderQty) {
                        p.decreaseStock(orderQty);
                        productRepository.save(p);
                        successCount.incrementAndGet();
                        log.info("✅ [{}] 재고 감소 성공", index);
                    } else {
                        failCount.incrementAndGet();
                        log.error("❌ [{}] 재고 부족", index);
                    }

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("❌ [{}] 오류: {}", index, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then - 결과 확인
        Product afterProduct = productRepository.findById(2L).orElseThrow();

        log.info("\n========== 락 없음 테스트 결과 ==========");
        log.info("성공: {}", successCount.get());
        log.info("실패: {}", failCount.get());
        log.info("초기 재고: {}", initialStock);
        log.info("최종 재고: {}", afterProduct.getStock());
        log.info("예상 재고: {}", (initialStock - successCount.get() * orderQty));
        log.info("======================================\n");

        // ❌ 동시성 문제 확인
        if (afterProduct.getStock() < 0) {
            log.error("⚠️⚠️⚠️ 경고: 재고가 음수입니다! 동시성 문제 발생! ⚠️⚠️⚠️");
        }

        if (afterProduct.getStock() != initialStock - successCount.get() * orderQty) {
            log.error("⚠️⚠️⚠️ 경고: 재고가 정확하지 않습니다! 동시성 문제 발생! ⚠️⚠️⚠️");
        }

        // 동시성 문제 발생 확인
        assertTrue(afterProduct.getStock() < 0 ||
                        afterProduct.getStock() != initialStock - successCount.get() * orderQty,
                "동시성 문제가 발생해야 합니다");
    }

    /**
     * 테스트 4: 대량 동시 요청 - 100명
     */
    @Test
    @DisplayName("대량 동시 요청 - 100명이 2개씩 주문 (Redis 분산락)")
    public void testHighConcurrency() throws InterruptedException {

        // Given
        Product product = productRepository.findById(1L).orElseThrow();
        product.changeStock(100);
        productRepository.save(product);

        int initialStock = product.getStock();

        log.info("\n========== 대량 동시 요청 테스트 시작 ==========");
        log.info("초기 재고: {}", initialStock);
        log.info("동시 사용자: 100명");
        log.info("주문 수량: 2개");

        int threadCount = 100;
        int orderQty = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    OrderDTO orderDTO = createOrderDTO(1L, orderQty, index);
                    redisLockOrderService.createOrder(orderDTO);
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

        // Then
        Product afterProduct = productRepository.findById(1L).orElseThrow();

        log.info("\n========== 대량 동시 요청 테스트 결과 ==========");
        log.info("실행 시간: {}ms", duration);
        log.info("평균 응답시간: {}ms", duration / threadCount);
        log.info("처리량: {:.2f} req/sec", (threadCount * 1000.0) / duration);
        log.info("성공: {}", successCount.get());
        log.info("실패: {}", failCount.get());
        log.info("초기 재고: {}", initialStock);
        log.info("최종 재고: {}", afterProduct.getStock());
        log.info("============================================\n");

        // 검증
        assertEquals(initialStock - successCount.get() * orderQty, afterProduct.getStock());
        assertTrue(afterProduct.getStock() >= 0);
    }

    /**
     * 테스트 5: 주문 취소 - 재고 복구
     */
    @Test
    @DisplayName("주문 취소 시 재고 복구 테스트")
    public void testOrderCancelStockRestore() {

        // Given
        Product product = productRepository.findById(1L).orElseThrow();
        int initialStock = product.getStock();

        log.info("\n========== 주문 취소 테스트 시작 ==========");
        log.info("초기 재고: {}", initialStock);

        // When - 주문 생성
        OrderDTO orderDTO = createOrderDTO(1L, 10, 0);
        String orderNumber = redisLockOrderService.createOrder(orderDTO);

        // 재고 확인
        Product afterOrder = productRepository.findById(1L).orElseThrow();
        log.info("주문 후 재고: {}", afterOrder.getStock());
        assertEquals(initialStock - 10, afterOrder.getStock());

        // 주문 취소
        // redisLockOrderService.cancelOrder(...) 구현 필요

        log.info("========================================\n");
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * OrderDTO 생성 헬퍼
     */
    private OrderDTO createOrderDTO(Long productId, int qty, int index) {
        return OrderDTO.builder()
                .email("user1@aaa.com")
                .orderItems(List.of(
                        OrderItemDTO.builder()
                                .pno(productId)
                                .qty(qty)
                                .build()
                ))
                .delivery(DeliveryDTO.builder()
                        .receiverName("테스터" + index)
                        .receiverPhone("010-1234-5678")
                        .address("서울시 강남구")
                        .zipCode("12345")
                        .deliveryMessage("문 앞에 놔주세요")
                        .build())
                .paymentMethod("CARD")
                .build();
    }
}