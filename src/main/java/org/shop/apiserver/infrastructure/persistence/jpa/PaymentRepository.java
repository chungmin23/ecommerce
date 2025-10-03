package org.shop.apiserver.infrastructure.persistence.jpa;

import org.shop.apiserver.domain.model.payment.Payment;
import org.shop.apiserver.domain.model.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // paymentKey로 조회
    Optional<Payment> findByPaymentKey(String paymentKey);

    // orderId(주문번호)로 조회
    Optional<Payment> findByOrderId(String orderId);

    // 주문 ID로 결제 정보 조회
    @Query("SELECT p FROM Payment p WHERE p.order.ono = :ono")
    Optional<Payment> findByOrderOno(@Param("ono") Long ono);

    // 주문번호로 결제 정보 조회 (주문과 함께)
    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN FETCH p.order " +
            "WHERE p.orderId = :orderId")
    Optional<Payment> findByOrderIdWithOrder(@Param("orderId") String orderId);

    // 결제 상태별 조회
    @Query("SELECT p FROM Payment p WHERE p.status = :status")
    List<Payment> findByStatus(@Param("status") PaymentStatus status);
}
