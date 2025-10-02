package org.shop.apiserver.repository;

import org.shop.apiserver.domain.OrderStatus;
import org.shop.apiserver.domain.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long> {
    // 주문번호로 조회
    Optional<Orders> findByOrderNumber(String orderNumber);

    // 회원의 주문 목록 조회 (페이징)
    @Query("SELECT o FROM Orders o WHERE o.member.email = :email ORDER BY o.orderDate DESC")
    Page<Orders> findByMemberEmail(@Param("email") String email, Pageable pageable);

    // 회원의 특정 상태 주문 목록
    @Query("SELECT o FROM Orders o WHERE o.member.email = :email AND o.status = :status ORDER BY o.orderDate DESC")
    Page<Orders> findByMemberEmailAndStatus(
            @Param("email") String email,
            @Param("status") OrderStatus status,
            Pageable pageable);

    // 주문 상세 조회 (OrderItem, Delivery 함께)
    @Query("SELECT o FROM Orders o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "LEFT JOIN FETCH o.delivery " +
            "WHERE o.ono = :ono")
    Optional<Orders> findByIdWithDetails(@Param("ono") Long ono);

    // 주문번호로 상세 조회
    @Query("SELECT o FROM Orders o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "LEFT JOIN FETCH o.delivery " +
            "WHERE o.orderNumber = :orderNumber")
    Optional<Orders> findByOrderNumberWithDetails(@Param("orderNumber") String orderNumber);
}
