package org.shop.apiserver.service;

import jakarta.transaction.Transactional;
import org.shop.apiserver.dto.PaymentResponseDTO;

@Transactional
public interface PaymentService {
    /**
     * 결제 처리 (주문 생성 시 자동 호출)
     * 주문 생성 → 즉시 결제 완료 처리
     */
    PaymentResponseDTO processPayment(String orderNumber, String paymentMethod);

    /**
     * 결제 취소
     */
    void cancelPayment(String orderNumber, String cancelReason);

    /**
     * 결제 조회
     */
    PaymentResponseDTO getPayment(Long paymentId);

    /**
     * 주문번호로 결제 조회
     */
    PaymentResponseDTO getPaymentByOrderNumber(String orderNumber);
}
