package org.shop.apiserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {

    private String email;  // 주문자 이메일

    @Builder.Default
    private List<OrderItemDTO> orderItems = new ArrayList<>();

    private DeliveryDTO delivery;  // 배송 정보

    private Long memberCouponId;  // 사용할 쿠폰 ID (옵션)
}