package org.shop.apiserver.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.shop.apiserver.domain.model.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {

    private Long ono;
    private String orderNumber;
    private String memberEmail;
    private int totalAmount;
    private int discountAmount;
    private int finalAmount;
    private LocalDateTime orderDate;
    private OrderStatus status;

    @Builder.Default
    private List<OrderItemDTO> orderItems = new ArrayList<>();

    private DeliveryResponseDTO delivery;
}