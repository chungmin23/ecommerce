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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 배치 처리 기능이 추가된 쿠폰 발급 Consumer
 * - 쿠폰 발급 요청을 배치로 모아서 처리
 * - DB 쓰기 성능 개선 (N+1 문제 해결)
 * - 메모리 효율성 증대
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class CouponKafkaConsumer {

    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final CouponIssueSagaService couponIssueSagaService;

    // 배치 처리용 큐
    private final ConcurrentLinkedQueue<CouponIssueEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, Coupon> couponCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Member> memberCache = new ConcurrentHashMap<>();

    private static final int BATCH_SIZE = 50;  // 한 번에 50개씩 처리

    /**
     * 쿠폰 발급 이벤트를 처리 (배치 모드)
     * 개별 이벤트를 큐에 추가하고, 일정 크기에 도달하면 배치 처리
     */
    @KafkaListener(
            topics = "${kafka.topics.coupon-issue:coupon-issue-events}",
            groupId = "${spring.kafka.consumer.group-id:coupon-service-group}",
            concurrency = "10"
    )
    public void handleCouponIssueEvent(@Payload CouponIssueEvent event) {
        try {
            log.debug("[CouponKafkaConsumer] 쿠폰 발급 이벤트 큐에 추가 - eventId: {}, email: {}",
                    event.getEventId(), event.getMemberEmail());

            eventQueue.offer(event);

            // 배치 크기에 도달하면 처리
            if (eventQueue.size() >= BATCH_SIZE) {
                processBatch();
            }

        } catch (Exception e) {
            log.error("[CouponKafkaConsumer] 이벤트 처리 중 예외 발생 - eventId: {}, error: {}",
                    event.getEventId(), e.getMessage(), e);
            updateEventStatus(event, "FAILED", e.getMessage());
        }
    }

    /**
     * 주기적으로 남은 이벤트를 처리하는 메서드
     * 스케줄러에 의해 호출됨 (예: 1초마다)
     */
    public void flushBatch() {
        if (!eventQueue.isEmpty()) {
            log.debug("[CouponKafkaConsumer] 남은 이벤트 플러시 - 큐 크기: {}", eventQueue.size());
            processBatch();
        }
    }

    /**
     * 배치 처리 메서드
     * BATCH_SIZE만큼 또는 큐의 모든 이벤트를 처리
     */
    @Transactional
    private void processBatch() {
        int batchSize = Math.min(BATCH_SIZE, eventQueue.size());
        
        for (int i = 0; i < batchSize; i++) {
            CouponIssueEvent event = eventQueue.poll();
            if (event != null) {
                processSingleEvent(event);
            }
        }

        log.info("[CouponKafkaConsumer] 배치 처리 완료 - 처리된 개수: {}, 남은 이벤트: {}",
                batchSize, eventQueue.size());
    }

    /**
     * 단일 이벤트 처리 (트랜잭션 내에서 실행)
     */
    private void processSingleEvent(CouponIssueEvent event) {
        try {
            log.debug("[CouponKafkaConsumer] 단일 이벤트 처리 - eventId: {}, email: {}, couponCode: {}",
                    event.getEventId(), event.getMemberEmail(), event.getCouponCode());

            // 1. 회원 조회 (캐시 활용)
            Member member = memberCache.computeIfAbsent(event.getMemberEmail(), email ->
                    memberRepository.findById(email)
                            .orElseThrow(() -> {
                                log.warn("[CouponKafkaConsumer] 회원 없음 - email: {}", email);
                                return new NoSuchElementException("회원 없음");
                            })
            );

            // 2. 쿠폰 조회 (캐시 활용)
            Coupon coupon = couponCache.computeIfAbsent(event.getCouponId(), couponId ->
                    couponRepository.findByCouponCode(event.getCouponCode())
                            .orElseThrow(() -> {
                                log.warn("[CouponKafkaConsumer] 쿠폰 없음 - couponCode: {}", event.getCouponCode());
                                return new NoSuchElementException("쿠폰 없음");
                            })
            );

            // 3. 쿠폰 유효성 검사
            if (!coupon.isAvailable()) {
                log.warn("[CouponKafkaConsumer] 사용 불가 쿠폰 - couponCode: {}", event.getCouponCode());
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

            // 5. 선착순 재고 확인 및 감소 (Redis 원자성 보장)
            long remainingStock = couponIssueSagaService.decrementCouponStock(event.getCouponId());

            if (remainingStock < 0) {
                log.warn("[CouponKafkaConsumer] 쿠폰 재고 부족 - couponId: {}", event.getCouponId());
                updateEventStatus(event, "FAILED", "쿠폰 재고 부족");
                return;
            }

            // 6. 쿠폰 발급 (배치 트랜잭션 내에서 처리)
            MemberCoupon memberCoupon = MemberCoupon.builder()
                    .member(member)
                    .coupon(coupon)
                    .used(false)
                    .build();

            memberCouponRepository.save(memberCoupon);

            log.debug("[CouponKafkaConsumer] 쿠폰 발급 완료 - eventId: {}, email: {}, remainingStock: {}",
                    event.getEventId(), event.getMemberEmail(), remainingStock);

            updateEventStatus(event, "SUCCESS", null);

        } catch (NoSuchElementException e) {
            log.error("[CouponKafkaConsumer] 리소스 없음 - eventId: {}, error: {}",
                    event.getEventId(), e.getMessage());
            updateEventStatus(event, "FAILED", e.getMessage());

        } catch (Exception e) {
            log.error("[CouponKafkaConsumer] 예외 발생 - eventId: {}, error: {}",
                    event.getEventId(), e.getMessage(), e);

            // 재시도 로직
            if (event.getRetryCount() < 3) {
                event.incrementRetry();
                eventQueue.offer(event);  // 다시 큐에 추가
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

    /**
     * 캐시 초기화 (선택적)
     */
    public void clearCache() {
        couponCache.clear();
        memberCache.clear();
        log.debug("[CouponKafkaConsumer] 캐시 초기화 완료");
    }
}