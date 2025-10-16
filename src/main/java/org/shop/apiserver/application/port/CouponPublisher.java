package org.shop.apiserver.application.port;

import org.shop.apiserver.domain.event.CouponIssueEvent;

/**
 * 쿠폰 발급 이벤트 발행 포트 (아웃바운드 포트)
 * 구현체에 따라 Kafka/Direct 선택 가능
 */
public interface CouponPublisher {
    void publish(CouponIssueEvent event);
}
