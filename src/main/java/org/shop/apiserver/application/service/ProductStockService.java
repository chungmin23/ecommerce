package org.shop.apiserver.application.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.domain.model.product.Product;
import org.shop.apiserver.infrastructure.persistence.jpa.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Log4j2
@RequiredArgsConstructor
public class ProductStockService {

    private final ProductRepository productRepository;
    private final RedisLockService redisLockService;

    /**
     * 상품 재고 감소 (분산락 + 별도 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Product decreaseStockWithLock(Long pno, int qty) {
        String lockKey = "product:lock:" + pno;

        return redisLockService.executeWithLock(lockKey, 5, 10, () -> {
            Product p = productRepository.findById(pno)
                    .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다."));

            if (p.getStock() < qty) {
                throw new IllegalStateException("재고 부족: 상품번호 " + pno);
            }

            p.decreaseStock(qty);
            productRepository.saveAndFlush(p); // ✅ DB 즉시 반영 (flush)
            log.info("✅ 재고 감소 완료: pno={}, 남은재고={}", pno, p.getStock());
            return p;
        });
    }

    /**
     * 상품 재고 복구 (취소 시)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Product increaseStockWithLock(Long pno, int qty) {
        String lockKey = "product:lock:" + pno;

        return redisLockService.executeWithLock(lockKey, 5, 10, () -> {
            Product p = productRepository.findById(pno)
                    .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다."));

            p.increaseStock(qty);
            productRepository.saveAndFlush(p);
            log.info("♻️ 재고 복구 완료: pno={}, 복구수량={}, 현재재고={}", pno, qty, p.getStock());
            return p;
        });
    }
}