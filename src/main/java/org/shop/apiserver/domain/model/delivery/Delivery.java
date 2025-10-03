package org.shop.apiserver.domain.model.delivery;

import jakarta.persistence.*;
import lombok.*;
import org.shop.apiserver.domain.model.order.Orders;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "order")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dno;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_ono", unique = true, nullable = false)
    private Orders order;

    @Column(nullable = false, length = 100)
    private String receiverName;      // 수령인 이름

    @Column(nullable = false, length = 20)
    private String receiverPhone;     // 수령인 전화번호

    @Column(nullable = false, length = 500)
    private String address;           // 배송 주소

    @Column(length = 10)
    private String zipCode;           // 우편번호

    @Column(length = 255)
    private String deliveryMessage;   // 배송 메시지

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.READY;

    @Column(length = 50)
    private String trackingNumber;    // 송장번호

    // 연관관계 편의 메서드
    public void setOrder(Orders order) {
        this.order = order;
    }

    // 배송 상태 변경
    public void changeStatus(DeliveryStatus status) {
        this.status = status;
    }

    // 송장번호 등록
    public void registerTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
        this.status = DeliveryStatus.SHIPPING;
    }
}
