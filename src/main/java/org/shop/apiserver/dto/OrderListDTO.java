package org.shop.apiserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.shop.apiserver.domain.OrderStatus;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderListDTO {

    private Long ono;
    private String orderNumber;
    private int finalAmount;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private String firstProductName;  // 대표 상품명
    private int productCount;         // 상품 개수
}