package org.shop.apiserver.infrastructure.persistence.jpa;

import org.shop.apiserver.domain.model.coupon.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCouponCode(String couponCode);
    List<Coupon> findByActiveTrue();
}
