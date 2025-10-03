package org.shop.apiserver.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.domain.*;
import org.shop.apiserver.dto.PaymentResponseDTO;
import org.shop.apiserver.repository.OrderRepository;
import org.shop.apiserver.repository.PaymentRepository;
import org.shop.apiserver.util.PaymentException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Override
    public PaymentResponseDTO processPayment(String orderNumber, String paymentMethod) {

        log.info("Process payment for order: " + orderNumber);

        // 1. 주문 조회
        Orders order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));

        // 2. 이미 결제가 있는지 확인
        if (order.getPayment() != null) {
            throw new PaymentException("이미 결제가 완료된 주문입니다.");
        }

        // 3. 결제 키 생성 (UUID)
        String paymentKey = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        // 4. 결제 수단 변환
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            method = PaymentMethod.CARD; // 기본값
        }

        // 5. Payment 엔티티 생성 및 즉시 승인 처리
        Payment payment = Payment.builder()
                .order(order)
                .paymentKey(paymentKey)
                .orderId(orderNumber)
                .amount(order.getFinalAmount())
                .method(method)
                .status(PaymentStatus.DONE)
                .requestedAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // 6. 주문 상태 변경 (PENDING → PAID)
        order.changeStatus(OrderStatus.PAID);

        log.info("Payment processed: " + paymentKey);

        return entityToDTO(payment);
    }

    @Override
    public void cancelPayment(String orderNumber, String cancelReason) {

        log.info("Cancel payment for order: " + orderNumber);

        // 1. Payment 조회
        Payment payment = paymentRepository.findByOrderId(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("결제 정보를 찾을 수 없습니다."));

        // 2. 취소 가능 상태 체크
        if (payment.getStatus() != PaymentStatus.DONE) {
            throw new PaymentException("취소할 수 없는 결제 상태입니다.");
        }

        // 3. Payment 상태 업데이트
        payment.cancel(cancelReason);

        // 4. 주문 상태 변경 (PAID → CANCELLED)
        Orders order = payment.getOrder();
        order.changeStatus(OrderStatus.CANCELLED);

        log.info("Payment cancelled: " + payment.getPaymentKey());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoSuchElementException("결제 정보를 찾을 수 없습니다."));

        return entityToDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByOrderNumber(String orderNumber) {

        Payment payment = paymentRepository.findByOrderId(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("결제 정보를 찾을 수 없습니다."));

        return entityToDTO(payment);
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    private PaymentResponseDTO entityToDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .paymentId(payment.getPaymentId())
                .paymentKey(payment.getPaymentKey())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .requestedAt(payment.getRequestedAt())
                .approvedAt(payment.getApprovedAt())
                .cancelledAt(payment.getCancelledAt())
                .failReason(payment.getFailReason())
                .cancelReason(payment.getCancelReason())
                .build();
    }
}