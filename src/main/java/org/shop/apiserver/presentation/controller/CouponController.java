package org.shop.apiserver.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.dto.CouponDTO;
import org.shop.apiserver.application.dto.MemberCouponDTO;
import org.shop.apiserver.application.facade.CouponFacade;
import org.shop.apiserver.application.service.CouponService;
import org.shop.apiserver.infrastructure.persistence.jpa.CouponRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Log4j2
public class CouponController {

    private final CouponService couponService;
    private final CouponFacade couponFacade;
    private final CouponRepository couponRepository;

    // 쿠폰 생성 (관리자)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/")
    public Map<String, Long> createCoupon(@RequestBody CouponDTO dto) {
        Long id = couponService.createCoupon(dto);
        return Map.of("couponId", id);
    }

    // 활성 쿠폰 목록
    @GetMapping("/active")
    public List<CouponDTO> getActiveCoupons() {
        return couponService.getActiveCoupons();
    }

    // 쿠폰 발급 (누구나 접근 가능 - SecurityConfig에서 permitAll로 설정)
    @PostMapping("/issue/{couponCode}")
    public Map<String, String> issueCoupon(
            @PathVariable String couponCode,
            Principal principal) {
        couponFacade.issueCouponAuto(principal.getName(), couponCode);
        return Map.of("result", "SUCCESS");
    }


    // 내 쿠폰 목록
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @GetMapping("/my")
    public List<MemberCouponDTO> getMyCoupons(Principal principal) {
        return couponService.getMyCoupons(principal.getName());
    }

    // 사용 가능한 쿠폰 조회 (주문 금액 기준)
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @GetMapping("/available")
    public List<CouponDTO> getAvailableCouponsForOrder(@RequestParam int orderAmount) {
        return couponFacade.getAvailableCouponsForOrder(orderAmount);
    }

    // 체크아웃용 사용 가능 쿠폰 조회
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @GetMapping("/checkout")
    public List<MemberCouponDTO> getUsableCouponsForCheckout(Principal principal) {
        return couponFacade.getUsableCouponsForCheckout(principal.getName());
    }


}
