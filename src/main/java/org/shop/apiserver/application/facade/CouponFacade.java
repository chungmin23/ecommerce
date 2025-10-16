package org.shop.apiserver.application.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.dto.CouponDTO;
import org.shop.apiserver.application.dto.MemberCouponDTO;
import org.shop.apiserver.application.service.CouponIssueSagaService;
import org.shop.apiserver.application.service.CouponService;
import org.shop.apiserver.domain.model.coupon.Coupon;
import org.shop.apiserver.infrastructure.persistence.jpa.CouponRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Coupon Facade
 * 
 * 역할:
 * - 다양한 CouponService 메서드를 조합하여 복잡한 비즈니스 로직 구현
 * - Controller와 Service 사이의 중간 계층
 * - 복잡한 쿠폰 처리 흐름을 캡슐화
 * 
 * 사용 예시:
 * - 선착순 쿠폰 발급 (재고 감소 포함)
 * - 여러 쿠폰 대량 발급
 * - 주문 적용 시 쿠폰 검증 및 할인 계산
 */
@Component
@Transactional
@RequiredArgsConstructor
@Log4j2
public class CouponFacade {

    private final CouponService couponService;
    private final CouponIssueSagaService sagaService;
    private final CouponRepository couponRepository;

    /**
     * 선착순 쿠폰 발급 (재고 감소 포함)
     * 
     * 처리 흐름:
     * 1. 재고 확인 및 감소
     * 2. 쿠폰 발급
     * 3. 로깅 및 예외 처리
     */
    public void issueLimitedCoupon(String email, String couponCode) {
        log.info("[CouponFacade] 선착순 쿠폰 발급 시작 - email: {}, couponCode: {}", email, couponCode);

        try {
            // 1. 쿠폰 ID 조회
            Coupon coupon = couponRepository.findByCouponCode(couponCode)
                    .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다"));

            // 2. 재고 확인 및 감소
            long remainingStock = sagaService.decrementCouponStock(coupon.getCouponId());

            if (remainingStock < 0) {
                log.warn("[CouponFacade] 재고 부족 - couponCode: {}", couponCode);
                throw new IllegalStateException("쿠폰이 모두 소진되었습니다");
            }

            // 3. 쿠폰 발급
            couponService.issueCoupon(email, couponCode);

            log.info("[CouponFacade] 선착순 쿠폰 발급 완료 - email: {}, remainingStock: {}", 
                    email, remainingStock);

        } catch (IllegalStateException e) {
            log.warn("[CouponFacade] 선착순 쿠폰 발급 실패 (재고 부족) - email: {}, error: {}", 
                    email, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[CouponFacade] 선착순 쿠폰 발급 실패 - email: {}, error: {}", 
                    email, e.getMessage(), e);
            throw new RuntimeException("선착순 쿠폰 발급 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 대량 쿠폰 발급 (여러 회원에게)
     * 
     * 처리 흐름:
     * 1. 각 회원별로 반복 처리
     * 2. 성공/실패 개수 카운트
     * 3. 결과 로깅
     * 
     * @return 성공한 쿠폰 발급 개수
     */
    public int issueBulkCoupons(List<String> emailList, String couponCode) {
        log.info("[CouponFacade] 대량 쿠폰 발급 시작 - count: {}, couponCode: {}", 
                emailList.size(), couponCode);

        int successCount = 0;
        int failCount = 0;

        for (String email : emailList) {
            try {
                couponService.issueCoupon(email, couponCode);
                successCount++;
            } catch (Exception e) {
                log.warn("[CouponFacade] 개별 발급 실패 - email: {}, error: {}", 
                        email, e.getMessage());
                failCount++;
            }
        }

        log.info("[CouponFacade] 대량 쿠폰 발급 완료 - success: {}, fail: {}, couponCode: {}", 
                successCount, failCount, couponCode);

        return successCount;
    }

    /**
     * 주문에 쿠폰 적용 (할인 계산 포함)
     * 
     * 처리 흐름:
     * 1. 쿠폰 사용 처리
     * 2. 할인 금액 계산
     * 3. 결과 반환
     */
    public int applyCouponToOrder(Long memberCouponId, String email, int orderAmount) {
        log.info("[CouponFacade] 주문에 쿠폰 적용 - email: {}, orderAmount: {}", email, orderAmount);

        try {
            int discount = couponService.useCoupon(memberCouponId, email, orderAmount);
            
            log.info("[CouponFacade] 쿠폰 적용 완료 - discount: {}", discount);

            return discount;

        } catch (Exception e) {
            log.error("[CouponFacade] 쿠폰 적용 실패 - email: {}, error: {}", email, e.getMessage());
            throw e;
        }
    }

    /**
     * 주문 금액에 맞는 사용 가능 쿠폰 조회
     * 
     * 처리 흐름:
     * 1. 활성 쿠폰 목록 조회
     * 2. 주문 금액에 맞는 쿠폰만 필터링
     */
    public List<CouponDTO> getAvailableCouponsForOrder(int orderAmount) {
        log.info("[CouponFacade] 주문 금액에 맞는 쿠폰 조회 - orderAmount: {}", orderAmount);

        List<CouponDTO> availableCoupons = couponService.getActiveCoupons().stream()
                .filter(coupon -> coupon.getMinOrderAmount() == null || 
                        orderAmount >= coupon.getMinOrderAmount())
                .toList();

        log.info("[CouponFacade] 조회 완료 - count: {}", availableCoupons.size());

        return availableCoupons;
    }

    /**
     * 내 쿠폰 중 사용 가능한 것만 조회
     */
    public List<MemberCouponDTO> getUsableCouponsForCheckout(String email) {
        log.info("[CouponFacade] 회원의 사용 가능 쿠폰 조회 - email: {}", email);

        List<MemberCouponDTO> usableCoupons = couponService.getMyCoupons(email).stream()
                .filter(MemberCouponDTO::isUsable)
                .toList();

        log.info("[CouponFacade] 조회 완료 - count: {}", usableCoupons.size());

        return usableCoupons;
    }
}
