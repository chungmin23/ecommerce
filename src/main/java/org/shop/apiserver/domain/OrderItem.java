package org.shop.apiserver.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"order", "product"})
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_ono", nullable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_pno", nullable = false)
    private Product product;

    private int qty;        // 주문 수량

    private int price;      // 주문 당시 가격 (가격 변동 대비)

    // 연관관계 편의 메서드
    public void setOrder(Orders order) {
        this.order = order;
    }

    // 총 가격 계산
    public int getTotalPrice() {
        return this.price * this.qty;
    }
}
