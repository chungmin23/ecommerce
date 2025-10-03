package org.shop.apiserver.infrastructure.persistence.jpa;

import org.shop.apiserver.domain.model.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // 주문의 모든 아이템 조회
    @Query("SELECT oi FROM OrderItem oi " +
            "LEFT JOIN FETCH oi.product " +
            "WHERE oi.order.ono = :ono")
    List<OrderItem> findByOrderId(@Param("ono") Long ono);

    // 상품별 주문 내역 조회
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.pno = :pno")
    List<OrderItem> findByProductId(@Param("pno") Long pno);
}
