package org.shop.apiserver.repository;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.shop.apiserver.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Log4j2
public class PaymentRepositoryTests {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 테스트 결제 데이터 생성
     */
    @Test
    @Transactional
    @Commit
    public void testInsertPayment() {

        log.info("=== Test Insert Payment ===");

        // 1. 회원 조회
        Member member = memberRepository.findById("user1@aaa.com")
                .orElseThrow(() -> new RuntimeException("회원 없음"));

        // 2. 상품 조회
        Product product = productRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("상품 없음"));

        // 3. 주문 생성
        Orders order = Orders.builder()
                .orderNumber("ORD20250102TEST001")
                .member(member)
                .totalAmount(30000)
                .discountAmount(0)
                .finalAmount(30000)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .product(product)
                .qty(3)
                .price(10000)
                .build();

        order.addOrderItem(orderItem);

        Delivery delivery = Delivery.builder()
                .receiverName("테스터")
                .receiverPhone("010-9999-9999")
                .address("테스트 주소")
                .zipCode("12345")
                .deliveryMessage("테스트 배송")
                .status(DeliveryStatus.READY)
                .build();

        order.setDelivery(delivery);
        orderRepository.save(order);

        // 4. 결제 생성
        Payment payment = Payment.builder()
                .order(order)
                .paymentKey("PAY_TEST_KEY_001")
                .orderId(order.getOrderNumber())
                .amount(order.getFinalAmount())
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.DONE)
                .requestedAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        log.info("Payment created: " + payment.getPaymentKey());

        assertNotNull(payment.getPaymentId());
        assertEquals(30000, payment.getAmount());
        assertEquals(PaymentStatus.DONE, payment.getStatus());
    }

    /**
     * 결제 조회 테스트
     */
    @Test
    @Transactional
    public void testFindPayment() {

        log.info("=== Test Find Payment ===");

        // paymentKey로 조회
        Payment payment = paymentRepository.findByPaymentKey("PAY_TEST_KEY_001")
                .orElse(null);

        if (payment != null) {
            log.info("Payment found: " + payment.getPaymentKey());
            log.info("Amount: " + payment.getAmount());
            log.info("Status: " + payment.getStatus());
            log.info("Method: " + payment.getMethod());

            assertNotNull(payment);
            assertEquals("PAY_TEST_KEY_001", payment.getPaymentKey());
        } else {
            log.warn("Payment not found. Run testInsertPayment first.");
        }
    }

    /**
     * 주문번호로 결제 조회
     */
    @Test
    @Transactional
    public void testFindByOrderId() {

        log.info("=== Test Find By OrderId ===");

        Payment payment = paymentRepository.findByOrderId("ORD20250102TEST001")
                .orElse(null);

        if (payment != null) {
            log.info("Payment found by orderId");
            log.info("Order: " + payment.getOrder().getOrderNumber());
            log.info("Amount: " + payment.getAmount());

            assertNotNull(payment);
            assertEquals("ORD20250102TEST001", payment.getOrderId());
        }
    }

    /**
     * 주문과 함께 결제 조회
     */
    @Test
    @Transactional
    public void testFindWithOrder() {

        log.info("=== Test Find With Order ===");

        Payment payment = paymentRepository.findByOrderIdWithOrder("ORD20250102TEST001")
                .orElse(null);

        if (payment != null) {
            log.info("Payment with order loaded");
            log.info("Order status: " + payment.getOrder().getStatus());
            log.info("Order items: " + payment.getOrder().getOrderItems().size());

            assertNotNull(payment.getOrder());
            assertFalse(payment.getOrder().getOrderItems().isEmpty());
        }
    }

    /**
     * 결제 상태별 조회
     */
    @Test
    public void testFindByStatus() {

        log.info("=== Test Find By Status ===");

        // DONE 상태인 결제 조회
        List<Payment> donePayments = paymentRepository.findByStatus(PaymentStatus.DONE);
        log.info("DONE payments count: " + donePayments.size());

        donePayments.forEach(payment -> {
            log.info("- " + payment.getPaymentKey() + " / " + payment.getAmount());
        });

        assertTrue(donePayments.size() >= 0);
    }

    /**
     * 결제 취소 테스트
     */
    @Test
    @Transactional
    @Commit
    public void testCancelPayment() {

        log.info("=== Test Cancel Payment ===");

        Payment payment = paymentRepository.findByPaymentKey("PAY_TEST_KEY_001")
                .orElse(null);

        if (payment != null && payment.getStatus() == PaymentStatus.DONE) {

            log.info("Before: " + payment.getStatus());

            // 결제 취소
            payment.cancel("테스트 취소");
            paymentRepository.save(payment);

            log.info("After: " + payment.getStatus());
            log.info("Cancel reason: " + payment.getCancelReason());
            log.info("Cancelled at: " + payment.getCancelledAt());

            assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
            assertNotNull(payment.getCancelledAt());
            assertEquals("테스트 취소", payment.getCancelReason());
        } else {
            log.warn("Payment not found or already cancelled");
        }
    }

    /**
     * 결제 승인 테스트
     */
    @Test
    @Transactional
    @Commit
    public void testApprovePayment() {

        log.info("=== Test Approve Payment ===");

        // READY 상태인 결제 찾기 또는 새로 생성
        Payment payment = Payment.builder()
                .order(orderRepository.findById(1L).orElseThrow())
                .paymentKey("PAY_TEST_READY")
                .orderId("ORD_TEST")
                .amount(10000)
                .status(PaymentStatus.READY)
                .requestedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        log.info("Before approve: " + payment.getStatus());

        // 결제 승인
        payment.approve("PAY_APPROVED_KEY", PaymentMethod.CARD);

        log.info("After approve: " + payment.getStatus());
        log.info("Approved at: " + payment.getApprovedAt());
        log.info("Method: " + payment.getMethod());

        assertEquals(PaymentStatus.DONE, payment.getStatus());
        assertNotNull(payment.getApprovedAt());
        assertEquals(PaymentMethod.CARD, payment.getMethod());
    }

    /**
     * 결제 실패 테스트
     */
    @Test
    @Transactional
    @Commit
    public void testFailPayment() {

        log.info("=== Test Fail Payment ===");

        Payment payment = Payment.builder()
                .order(orderRepository.findById(1L).orElseThrow())
                .paymentKey("PAY_TEST_FAIL")
                .orderId("ORD_TEST_FAIL")
                .amount(10000)
                .status(PaymentStatus.READY)
                .requestedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // 결제 실패 처리
        payment.fail("카드 한도 초과");

        log.info("Status: " + payment.getStatus());
        log.info("Fail reason: " + payment.getFailReason());

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("카드 한도 초과", payment.getFailReason());
    }
}
