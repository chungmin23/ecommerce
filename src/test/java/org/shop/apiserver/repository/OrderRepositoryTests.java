package org.shop.apiserver.repository;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.shop.apiserver.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@SpringBootTest
@Log4j2
public class OrderRepositoryTests {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 테스트 주문 생성
     */
    @Test
    @Transactional
    @Commit
    public void testInsertOrder() {

        // 1. 회원 조회
        Member member = memberRepository.findById("user1@aaa.com")
                .orElseThrow();

        // 2. 상품 조회
        Product product1 = productRepository.findById(1L).orElseThrow();
        Product product2 = productRepository.findById(2L).orElseThrow();

        // 3. 주문 생성
        Orders order = Orders.builder()
                .orderNumber("ORD20250102123456001")
                .member(member)
                .totalAmount(30000)
                .discountAmount(0)
                .finalAmount(30000)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .build();

        // 4. 주문 아이템 추가
        OrderItem item1 = OrderItem.builder()
                .product(product1)
                .qty(1)
                .price(10000)
                .build();

        OrderItem item2 = OrderItem.builder()
                .product(product2)
                .qty(2)
                .price(10000)
                .build();

        order.addOrderItem(item1);
        order.addOrderItem(item2);

        // 5. 배송 정보 추가
        Delivery delivery = Delivery.builder()
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .address("서울시 강남구 테헤란로 123")
                .zipCode("12345")
                .deliveryMessage("문 앞에 놔주세요")
                .status(DeliveryStatus.READY)
                .build();

        order.setDelivery(delivery);

        // 6. 주문 저장
        orderRepository.save(order);

        log.info("Order created: " + order.getOrderNumber());
    }

    /**
     * 주문 조회 테스트
     */
    @Test
    @Transactional
    public void testSelectOrder() {

        Long ono = 1L;

        Orders order = orderRepository.findByIdWithDetails(ono)
                .orElseThrow();

        log.info("Order: " + order);
        log.info("Order Items: " + order.getOrderItems().size());
        log.info("Delivery: " + order.getDelivery());
    }

    /**
     * 회원별 주문 목록 조회 테스트
     */
    @Test
    public void testSelectOrdersByMember() {

        String email = "user1@aaa.com";

        orderRepository.findByMemberEmail(
                email,
                org.springframework.data.domain.PageRequest.of(0, 10)
        ).getContent().forEach(order -> {
            log.info(order.getOrderNumber() + " - " + order.getFinalAmount());
        });
    }

    /**
     * 주문 상태 변경 테스트
     */
    @Test
    @Transactional
    @Commit
    public void testUpdateOrderStatus() {

        Long ono = 1L;

        Orders order = orderRepository.findById(ono).orElseThrow();

        log.info("Before: " + order.getStatus());

        order.changeStatus(OrderStatus.PAID);

        log.info("After: " + order.getStatus());
    }

    /**
     * 주문 취소 테스트
     */
    @Test
    @Transactional
    @Commit
    public void testCancelOrder() {

        Long ono = 1L;

        Orders order = orderRepository.findById(ono).orElseThrow();

        if (order.canCancel()) {
            order.changeStatus(OrderStatus.CANCELLED);
            log.info("Order cancelled: " + order.getOrderNumber());
        } else {
            log.error("Cannot cancel order");
        }
    }
}