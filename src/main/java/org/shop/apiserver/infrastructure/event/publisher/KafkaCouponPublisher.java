package org.shop.apiserver.infrastructure.event.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.port.CouponPublisher;
import org.shop.apiserver.application.service.CouponKafkaProducer;
import org.shop.apiserver.domain.event.CouponIssueEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka 기반 쿠폰 발급 구현
 * application.yml에서 coupon.issue.async=true일 때만 활성화
 */
@Component
@ConditionalOnProperty(name = "coupon.issue.async", havingValue = "true")
@RequiredArgsConstructor
@Log4j2
public class KafkaCouponPublisher implements CouponPublisher {

    private final CouponKafkaProducer kafkaProducer;

    @Override
    public void publish(CouponIssueEvent event) {
        log.info("[KafkaCouponPublisher] 쿠폰 발급 이벤트 발행 - eventId: {}, email: {}", 
                event.getEventId(), event.getMemberEmail());
        kafkaProducer.publishCouponIssueEvent(event);
    }
}
