package org.shop.apiserver.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberCouponDTO {
    private Long memberCouponId;
    private CouponDTO coupon;
    private boolean used;
    private boolean usable;
}
