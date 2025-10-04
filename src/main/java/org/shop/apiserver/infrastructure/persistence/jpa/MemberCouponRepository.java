package org.shop.apiserver.infrastructure.persistence.jpa;

import org.shop.apiserver.domain.model.coupon.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    @Query("SELECT mc FROM MemberCoupon mc " +
            "LEFT JOIN FETCH mc.coupon " +
            "WHERE mc.member.email = :email " +
            "AND mc.used = false")
    List<MemberCoupon> findUsableCouponsByEmail(@Param("email") String email);

    @Query("SELECT mc FROM MemberCoupon mc " +
            "LEFT JOIN FETCH mc.coupon " +
            "WHERE mc.memberCouponId = :id AND mc.member.email = :email")
    Optional<MemberCoupon> findByIdAndEmail(@Param("id") Long id, @Param("email") String email);
}