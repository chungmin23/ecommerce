package org.shop.apiserver.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.domain.event.CouponIssueEvent;
import org.shop.apiserver.domain.model.coupon.CouponIssueSaga;
import org.shop.apiserver.infrastructure.persistence.jpa.CouponIssueSagaRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 쿠폰 발급 SAGA 패턴 구현
 * 분산 트랜잭션 관리 및 선착순 재고 관리
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class CouponIssueSagaService {

    private final CouponIssueSagaRepository couponIssueSagaRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String COUPON_STOCK_PREFIX = "coupon:stock:";
    private static final String COUPON_ISSUED_PREFIX = "coupon:issued:";

    /**
     * 쿠폰 발급 이벤트 저장
     */
    @Transactional
    public void saveCouponIssueEvent(CouponIssueEvent event) {
        try {
            CouponIssueSaga saga = CouponIssueSaga.builder()
                    .eventId(event.getEventId())
                    .memberEmail(event.getMemberEmail())
                    .couponCode(event.getCouponCode())
                    .couponId(event.getCouponId())
                    .status(event.getStatus())
                    .errorMessage(event.getErrorMessage())
                    .retryCount(event.getRetryCount())
                    .timestamp(event.getTimestamp())
                    .build();

            couponIssueSagaRepository.save(saga);
            log.debug("[CouponIssueSagaService] SAGA 이벤트 저장 - eventId: {}, status: {}",
                    event.getEventId(), event.getStatus());

        } catch (Exception e) {
            log.error("[CouponIssueSagaService] SAGA 이벤트 저장 실패 - eventId: {}, error: {}",
                    event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 쿠폰 재고 초기화 (Redis에 저장)
     * @param couponId 쿠폰 ID
     * @param stock 초기 재고
     */
    public void initializeCouponStock(Long couponId, Long stock) {
        try {
            String stockKey = COUPON_STOCK_PREFIX + couponId;
            redisTemplate.opsForValue().set(stockKey, stock, 30, TimeUnit.DAYS);
            log.info("[CouponIssueSagaService] 쿠폰 재고 초기화 - couponId: {}, stock: {}", couponId, stock);
        } catch (Exception e) {
            log.error("[CouponIssueSagaService] 쿠폰 재고 초기화 실패 - couponId: {}, error: {}",
                    couponId, e.getMessage(), e);
        }
    }

    /**
     * 쿠폰 재고 감소 (선착순 처리)
     * Redis의 원자성을 이용해 동시성 제어
     * @param couponId 쿠폰 ID
     * @return 남은 재고
     */
    public long decrementCouponStock(Long couponId) {
        try {
            String stockKey = COUPON_STOCK_PREFIX + couponId;
            Long remaining = redisTemplate.opsForValue().decrement(stockKey);

            if (remaining != null && remaining >= 0) {
                log.debug("[CouponIssueSagaService] 쿠폰 재고 감소 - couponId: {}, remaining: {}", couponId, remaining);
                return remaining;
            } else {
                log.warn("[CouponIssueSagaService] 쿠폰 재고 부족 - couponId: {}, remaining: {}", couponId, remaining);
                return -1;
            }

        } catch (Exception e) {
            log.error("[CouponIssueSagaService] 쿠폰 재고 감소 실패 - couponId: {}, error: {}",
                    couponId, e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 쿠폰 현재 재고 조회
     */
    public long getCouponStock(Long couponId) {
        try {
            String stockKey = COUPON_STOCK_PREFIX + couponId;
            Object stock = redisTemplate.opsForValue().get(stockKey);
            return stock != null ? Long.parseLong(stock.toString()) : 0;
        } catch (Exception e) {
            log.error("[CouponIssueSagaService] 쿠폰 재고 조회 실패 - couponId: {}, error: {}",
                    couponId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 회원 쿠폰 발급 여부 확인 (Redis 캐시)
     */
    public boolean isAlreadyIssued(String memberEmail, Long couponId) {
        try {
            String issuedKey = COUPON_ISSUED_PREFIX + couponId + ":" + memberEmail;
            return redisTemplate.hasKey(issuedKey);
        } catch (Exception e) {
            log.error("[CouponIssueSagaService] 발급 여부 확인 실패 - email: {}, couponId: {}, error: {}",
                    memberEmail, couponId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 회원 쿠폰 발급 기록 (Redis 캐시)
     */
    public void markAsIssued(String memberEmail, Long couponId) {
        try {
            String issuedKey = COUPON_ISSUED_PREFIX + couponId + ":" + memberEmail;
            redisTemplate.opsForValue().set(issuedKey, "issued", 30, TimeUnit.DAYS);
            log.debug("[CouponIssueSagaService] 쿠폰 발급 기록 - email: {}, couponId: {}", memberEmail, couponId);
        } catch (Exception e) {
            log.error("[CouponIssueSagaService] 쿠폰 발급 기록 실패 - email: {}, couponId: {}, error: {}",
                    memberEmail, couponId, e.getMessage(), e);
        }
    }
}
