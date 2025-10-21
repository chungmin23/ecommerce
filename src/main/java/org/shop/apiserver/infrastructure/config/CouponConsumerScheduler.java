package org.shop.apiserver.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.service.CouponKafkaConsumer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 쿠폰 발급 Consumer 배치 처리 스케줄러
 * 1초마다 남은 이벤트를 처리
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class CouponConsumerScheduler {

    private final CouponKafkaConsumer couponKafkaConsumer;

    /**
     * 1초마다 배치 플러시
     * Consumer 큐에 남아있는 이벤트를 처리
     */
    @Scheduled(fixedRate = 1000)  // 1초마다 실행
    public void flushBatch() {
        try {
            couponKafkaConsumer.flushBatch();
        } catch (Exception e) {
            log.error("[CouponConsumerScheduler] 배치 플러시 중 예외 발생 - error: {}", 
                    e.getMessage(), e);
        }
    }

    /**
     * 10초마다 캐시 초기화 (선택사항)
     * 오래된 캐시 데이터를 제거하고 메모리 최적화
     */
    @Scheduled(fixedRate = 10000)  // 10초마다 실행
    public void clearCache() {
        try {
            couponKafkaConsumer.clearCache();
        } catch (Exception e) {
            log.error("[CouponConsumerScheduler] 캐시 초기화 중 예외 발생 - error: {}", 
                    e.getMessage(), e);
        }
    }
}
