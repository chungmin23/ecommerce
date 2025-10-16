package org.shop.apiserver.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.domain.event.CouponIssueEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 쿠폰 발급 이벤트를 Kafka로 발행하는 Producer
 * 비동기 처리로 API 응답 시간 개선
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class CouponKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.coupon-issue:coupon-issue-events}")
    private String couponIssueTopic;

    /**
     * 쿠폰 발급 이벤트를 Kafka에 발행
     * @param event 쿠폰 발급 이벤트
     */
    public void publishCouponIssueEvent(CouponIssueEvent event) {
        try {
            Message<CouponIssueEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader("kafka_topic", couponIssueTopic)
                    .setHeader("kafka_messageKey", event.getMemberEmail())
                    .setHeader("eventId", event.getEventId())
                    .setHeader("timestamp", System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("[CouponKafkaProducer] 쿠폰 발급 이벤트 발행 성공 - eventId: {}, email: {}, couponCode: {}",
                                    event.getEventId(), event.getMemberEmail(), event.getCouponCode());
                        } else {
                            log.error("[CouponKafkaProducer] 쿠폰 발급 이벤트 발행 실패 - eventId: {}, email: {}, error: {}",
                                    event.getEventId(), event.getMemberEmail(), ex.getMessage(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("[CouponKafkaProducer] 예외 발생 - eventId: {}, email: {}, error: {}",
                    event.getEventId(), event.getMemberEmail(), e.getMessage(), e);
        }
    }
}
