package org.shop.apiserver.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"orderItems", "delivery"})
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ono;

    @Column(unique = true, nullable = false, length = 50)
    private String orderNumber;  // 주문번호 (예: ORD20250102123456)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_email", nullable = false)
    private Member member;

    private int totalAmount;      // 총 상품 금액

    @Builder.Default
    private int discountAmount = 0;  // 할인 금액 (쿠폰 등)

    private int finalAmount;      // 최종 결제 금액

    @Builder.Default
    private LocalDateTime orderDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Delivery delivery;

    // 주문 아이템 추가 헬퍼 메서드
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    // 배송 정보 설정 헬퍼 메서드
    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        delivery.setOrder(this);
    }

    // 주문 상태 변경
    public void changeStatus(OrderStatus status) {
        this.status = status;
    }

    // 최종 금액 계산
    public void calculateFinalAmount() {
        this.finalAmount = this.totalAmount - this.discountAmount;
    }

    // 주문 취소 가능 여부 체크
    public boolean canCancel() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.PAID;
    }
}
