package org.shop.apiserver.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.shop.apiserver.domain.model.payment.PaymentMethod;
import org.shop.apiserver.domain.model.payment.PaymentStatus;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDTO {

    private Long paymentId;
    private String paymentKey;
    private String orderId;
    private int amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime cancelledAt;
    private String failReason;
    private String cancelReason;
}