package org.shop.apiserver.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 쿠폰 발급 이벤트
 * Kafka로 전송되는 이벤트 메시지
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CouponIssueEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("member_email")
    private String memberEmail;

    @JsonProperty("coupon_code")
    private String couponCode;

    @JsonProperty("coupon_id")
    private Long couponId;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("status")
    private String status; // PENDING, SUCCESS, FAILED

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("retry_count")
    private Integer retryCount;

    public static CouponIssueEvent pending(String memberEmail, String couponCode, Long couponId) {
        return CouponIssueEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .memberEmail(memberEmail)
                .couponCode(couponCode)
                .couponId(couponId)
                .timestamp(LocalDateTime.now())
                .status("PENDING")
                .retryCount(0)
                .build();
    }

    public CouponIssueEvent success() {
        this.status = "SUCCESS";
        return this;
    }

    public CouponIssueEvent failed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        return this;
    }

    public CouponIssueEvent incrementRetry() {
        this.retryCount = (retryCount == null) ? 1 : retryCount + 1;
        return this;
    }
}
