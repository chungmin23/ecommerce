package org.shop.apiserver.repository;

import org.shop.apiserver.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // 주문의 배송 정보 조회
    @Query("SELECT d FROM Delivery d WHERE d.order.ono = :ono")
    Optional<Delivery> findByOrderId(@Param("ono") Long ono);

    // 송장번호로 조회
    Optional<Delivery> findByTrackingNumber(String trackingNumber);
}
