package org.shop.apiserver.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "order")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_ono", unique = true, nullable = false)
    private Orders order;

    @Column(unique = true, nullable = false, length = 200)
    private String paymentKey;

    @Column(nullable = false, length = 100)
    private String orderId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.READY;

    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String failReason;

    @Column(length = 500)
    private String cancelReason;

    // 비즈니스 로직
    public void changeStatus(PaymentStatus status) {
        this.status = status;
    }

    public void approve(String paymentKey, PaymentMethod method) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.status = PaymentStatus.DONE;
        this.approvedAt = LocalDateTime.now();
    }

    public void cancel(String cancelReason) {
        this.status = PaymentStatus.CANCELLED;
        this.cancelReason = cancelReason;
        this.cancelledAt = LocalDateTime.now();
    }

    public void fail(String failReason) {
        this.status = PaymentStatus.FAILED;
        this.failReason = failReason;
    }

    public boolean canCancel() {
        return this.status == PaymentStatus.DONE;
    }
}
