package org.shop.apiserver.domain.model.coupon;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 쿠폰 발급 SAGA 패턴 이벤트 저장소
 * 비동기 쿠폰 발급 처리의 상태를 추적하고 재시도를 관리
 */
@Entity
@Table(name = "coupon_issue_saga", indexes = {
        @Index(name = "idx_event_id", columnList = "event_id"),
        @Index(name = "idx_member_email", columnList = "member_email"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CouponIssueSaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sagaId;

    @Column(name = "event_id", unique = true, nullable = false)
    private String eventId;

    @Column(name = "member_email", nullable = false)
    private String memberEmail;

    @Column(name = "coupon_code", nullable = false)
    private String couponCode;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, SUCCESS, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 상태 변경 메서드
    public void markSuccess() {
        this.status = "SUCCESS";
    }

    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
    }

    public void incrementRetry() {
        this.retryCount = (retryCount == null) ? 1 : retryCount + 1;
    }

    public boolean canRetry() {
        return retryCount < 3;
    }

    public boolean isCompleted() {
        return "SUCCESS".equals(status) || "FAILED".equals(status);
    }
}
