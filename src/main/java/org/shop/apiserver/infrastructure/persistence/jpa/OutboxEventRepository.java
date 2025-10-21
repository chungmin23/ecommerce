package org.shop.apiserver.infrastructure.persistence.jpa;

import org.shop.apiserver.domain.model.outbox.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.status = 'PENDING' " +
           "ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findPendingEvents();

    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.status = 'FAILED' " +
           "AND oe.retryCount < 3 ORDER BY oe.createdAt ASC")
    List<OutboxEvent> findFailedEventsForRetry();

    Optional<OutboxEvent> findByEventId(String eventId);
}
