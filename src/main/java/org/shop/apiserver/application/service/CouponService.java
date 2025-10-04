package org.shop.apiserver.application.service;

import jakarta.transaction.Transactional;
import org.shop.apiserver.application.dto.CouponDTO;
import org.shop.apiserver.application.dto.MemberCouponDTO;

import java.util.List;

@Transactional
public interface CouponService {

    // 쿠폰 생성 (관리자)
    Long createCoupon(CouponDTO couponDTO);

    // 활성 쿠폰 목록
    List<CouponDTO> getActiveCoupons();

    // 쿠폰 발급
    void issueCoupon(String email, String couponCode);

    // 내 쿠폰 목록
    List<MemberCouponDTO> getMyCoupons(String email);

    // 쿠폰 사용 (할인금액 반환)
    int useCoupon(Long memberCouponId, String email, int orderAmount);
}