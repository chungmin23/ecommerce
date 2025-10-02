package org.shop.apiserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO {

    private Long pno;      // 상품 번호
    private String pname;  // 상품명
    private int qty;       // 주문 수량
    private int price;     // 주문 당시 가격
}