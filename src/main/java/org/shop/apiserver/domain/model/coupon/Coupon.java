package org.shop.apiserver.domain.model.coupon;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Column(unique = true, nullable = false)
    private String couponCode;  // 쿠폰 코드

    @Column(nullable = false)
    private String couponName;  // 쿠폰 이름

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType couponType;  // FIXED(고정금액), PERCENT(퍼센트)

    @Column(nullable = false)
    private int discountValue;  // 할인값

    private Integer minOrderAmount;  // 최소 주문 금액

    @Column(nullable = false)
    private LocalDateTime endDate;  // 만료일

    @Builder.Default
    private boolean active = true;  // 활성화 여부

    @Builder.Default
    @Column(nullable = false)
    private int issuedCount = 0;  // 발급된 쿠폰 수

    @Column(nullable = false)
    private int maxIssueCount;  // 최대 발급 가능 수

    // 사용 가능 여부
    public boolean isAvailable() {
        return active && LocalDateTime.now().isBefore(endDate);
    }

    // 선착순 쿠폰 여부 (수량 제한이 있는 쿠폰)
    public boolean isLimitedStock() {
        return maxIssueCount > 0;
    }

    // 할인 금액 계산
    public int calculateDiscount(int orderAmount) {
        if (!isAvailable()) return 0;
        if (minOrderAmount != null && orderAmount < minOrderAmount) return 0;

        if (couponType == CouponType.FIXED) {
            return Math.min(discountValue, orderAmount);
        } else {
            return (int) (orderAmount * discountValue / 100.0);
        }
    }
}