package org.shop.apiserver.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.shop.apiserver.domain.model.coupon.CouponType;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponDTO {
    private Long couponId;
    private String couponCode;
    private String couponName;
    private CouponType couponType;
    private int discountValue;
    private Integer minOrderAmount;
    private LocalDateTime endDate;
    private boolean active;
    private Long stock;  // 선착순 쿠폰의 남은 재고
}
