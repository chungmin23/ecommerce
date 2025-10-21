package org.shop.apiserver.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.domain.model.outbox.OutboxEvent;
import org.shop.apiserver.infrastructure.persistence.jpa.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class OutboxEventCreatorService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OutboxEvent createOutboxEvent(String eventType, Object eventPayload) {

        try {
            String jsonPayload = objectMapper.writeValueAsString(eventPayload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventType(eventType)
                    .eventId(UUID.randomUUID().toString())
                    .payload(jsonPayload)
                    .status("PENDING")
                    .retryCount(0)
                    .build();

            OutboxEvent savedEvent = outboxRepository.save(outboxEvent);

            log.info("[OutboxEventCreator] 📝 Outbox 이벤트 생성 - eventId: {}, type: {}", 
                    savedEvent.getEventId(), eventType);

            return savedEvent;

        } catch (Exception e) {
            log.error("[OutboxEventCreator] ❌ Outbox 이벤트 생성 실패 - {}", e.getMessage(), e);
            throw new RuntimeException("Outbox 이벤트 생성 실패", e);
        }
    }
}
