package org.shop.apiserver.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.domain.model.outbox.OutboxEvent;
import org.shop.apiserver.infrastructure.persistence.jpa.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class OutboxPublisherService {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        log.debug("[OutboxPublisher] PENDING 이벤트 발행 시작");

        try {
            List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents();

            if (pendingEvents.isEmpty()) {
                log.debug("[OutboxPublisher] PENDING 이벤트 없음");
                return;
            }

            log.info("[OutboxPublisher] 발행할 이벤트: {}개", pendingEvents.size());

            for (OutboxEvent event : pendingEvents) {
                publishEvent(event);
            }

        } catch (Exception e) {
            log.error("[OutboxPublisher] 이벤트 발행 중 오류 - {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 3000)
    public void retryFailedEvents() {

        log.debug("[OutboxPublisher] FAILED 이벤트 재시도 시작");

        try {
            List<OutboxEvent> failedEvents = outboxRepository.findFailedEventsForRetry();

            if (failedEvents.isEmpty()) {
                log.debug("[OutboxPublisher] 재시도할 FAILED 이벤트 없음");
                return;
            }

            log.warn("[OutboxPublisher] 재시도 이벤트: {}개", failedEvents.size());

            for (OutboxEvent event : failedEvents) {
                publishEvent(event);
            }

        } catch (Exception e) {
            log.error("[OutboxPublisher] FAILED 이벤트 재시도 중 오류 - {}", e.getMessage(), e);
        }
    }

    private void publishEvent(OutboxEvent event) {

        try {
            Message<String> message = MessageBuilder
                    .withPayload(event.getPayload())
                    .setHeader(KafkaHeaders.TOPIC, event.getEventType())
                    .setHeader("eventId", event.getEventId())
                    .setHeader("timestamp", System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(message).get();

            event.markAsPublished();
            outboxRepository.save(event);

            log.info("[OutboxPublisher] ✅ 이벤트 발행 성공 - eventId: {}, topic: {}", 
                    event.getEventId(), event.getEventType());

        } catch (Exception e) {

            log.error("[OutboxPublisher] ❌ 이벤트 발행 실패 - eventId: {}, error: {}", 
                    event.getEventId(), e.getMessage());

            event.incrementRetry();

            if (event.canRetry()) {
                outboxRepository.save(event);
                log.warn("[OutboxPublisher] 재시도 대기 - eventId: {}, retry: {}/3", 
                        event.getEventId(), event.getRetryCount());
            } else {
                event.markAsFailed(e.getMessage());
                outboxRepository.save(event);
                log.error("[OutboxPublisher] ⚠️ 최대 재시도 초과 - eventId: {}", 
                        event.getEventId());
            }
        }
    }
}
