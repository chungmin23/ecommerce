package org.shop.apiserver.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.domain.event.CouponIssueEvent;
import org.shop.apiserver.domain.model.coupon.Coupon;
import org.shop.apiserver.domain.model.coupon.MemberCoupon;
import org.shop.apiserver.domain.model.member.Member;
import org.shop.apiserver.infrastructure.persistence.jpa.CouponRepository;
import org.shop.apiserver.infrastructure.persistence.jpa.MemberCouponRepository;
import org.shop.apiserver.infrastructure.persistence.jpa.MemberRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * 쿠폰 발급 이벤트를 처리하는 Kafka Consumer
 * 비동기로 쿠폰을 발급받음
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class CouponKafkaConsumer {

    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final CouponIssueSagaService couponIssueSagaService;

    /**
     * 쿠폰 발급 이벤트를 처리
     * 선착순 제한 체크 + 쿠폰 발급
     */
    @KafkaListener(
            topics = "${kafka.topics.coupon-issue:coupon-issue-events}",
            groupId = "${spring.kafka.consumer.group-id:coupon-service-group}",
            concurrency = "3"
    )
    @Transactional
    public void handleCouponIssueEvent(
            @Payload CouponIssueEvent event) {

        try {
            log.info("[CouponKafkaConsumer] 쿠폰 발급 이벤트 처리 시작 - eventId: {}, email: {}, couponCode: {}",
                    event.getEventId(), event.getMemberEmail(), event.getCouponCode());

            // 1. 회원 조회
            Member member = memberRepository.findById(event.getMemberEmail())
                    .orElseThrow(() -> {
                        log.warn("[CouponKafkaConsumer] 회원 없음 - email: {}", event.getMemberEmail());
                        return new NoSuchElementException("회원 없음");
                    });

            // 2. 쿠폰 조회
            Coupon coupon = couponRepository.findByCouponCode(event.getCouponCode())
                    .orElseThrow(() -> {
                        log.warn("[CouponKafkaConsumer] 쿠폰 없음 - couponCode: {}", event.getCouponCode());
                        return new NoSuchElementException("쿠폰 없음");
                    });

            // 3. 쿠폰 유효성 검사
            if (!coupon.isAvailable()) {
                log.warn("[CouponKafkaConsumer] 사용 불가 쿠폰 - couponCode: {}, isActive: {}",
                        event.getCouponCode(), coupon.isActive());
                updateEventStatus(event, "FAILED", "사용 불가 쿠폰");
                return;
            }

            // 4. 이미 발급받은 쿠폰인지 확인 (중복 발급 방지)
            boolean alreadyIssued = memberCouponRepository
                    .existsByMemberEmailAndCouponCouponCode(event.getMemberEmail(), event.getCouponCode());

            if (alreadyIssued) {
                log.warn("[CouponKafkaConsumer] 이미 발급된 쿠폰 - email: {}, couponCode: {}",
                        event.getMemberEmail(), event.getCouponCode());
                updateEventStatus(event, "FAILED", "이미 발급된 쿠폰");
                return;
            }

            // 5. 선착순 재고 확인 및 감소 (동시성 제어)
            long remainingStock = couponIssueSagaService.decrementCouponStock(event.getCouponId());

            if (remainingStock < 0) {
                log.warn("[CouponKafkaConsumer] 쿠폰 재고 부족 - couponId: {}, remaining: {}",
                        event.getCouponId(), remainingStock);
                updateEventStatus(event, "FAILED", "쿠폰 재고 부족");
                return;
            }

            // 6. 쿠폰 발급
            MemberCoupon memberCoupon = MemberCoupon.builder()
                    .member(member)
                    .coupon(coupon)
                    .used(false)
                    .build();

            memberCouponRepository.save(memberCoupon);

            log.info("[CouponKafkaConsumer] 쿠폰 발급 완료 - eventId: {}, email: {}, couponCode: {}, memberCouponId: {}, remainingStock: {}",
                    event.getEventId(), event.getMemberEmail(), event.getCouponCode(), memberCoupon.getMemberCouponId(), remainingStock);

            updateEventStatus(event, "SUCCESS", null);

        } catch (NoSuchElementException e) {
            log.error("[CouponKafkaConsumer] NoSuchElementException 발생 - eventId: {}, email: {}, error: {}",
                    event.getEventId(), event.getMemberEmail(), e.getMessage());
            updateEventStatus(event, "FAILED", e.getMessage());

        } catch (Exception e) {
            log.error("[CouponKafkaConsumer] 예외 발생 - eventId: {}, email: {}, error: {}",
                    event.getEventId(), event.getMemberEmail(), e.getMessage(), e);

            // 재시도 횟수 확인
            if (event.getRetryCount() < 3) {
                event.incrementRetry();
                log.info("[CouponKafkaConsumer] 재시도 - eventId: {}, retry_count: {}",
                        event.getEventId(), event.getRetryCount());
            } else {
                updateEventStatus(event, "FAILED", "최대 재시도 횟수 초과: " + e.getMessage());
            }
        }
    }

    /**
     * 이벤트 상태 업데이트
     */
    private void updateEventStatus(CouponIssueEvent event, String status, String errorMessage) {
        try {
            if ("SUCCESS".equals(status)) {
                event.success();
                couponIssueSagaService.saveCouponIssueEvent(event);
            } else if ("FAILED".equals(status)) {
                event.failed(errorMessage);
                couponIssueSagaService.saveCouponIssueEvent(event);
            }
        } catch (Exception e) {
            log.error("[CouponKafkaConsumer] 이벤트 상태 업데이트 실패 - eventId: {}, error: {}",
                    event.getEventId(), e.getMessage(), e);
        }
    }
}
