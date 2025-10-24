package org.shop.apiserver.application.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.dto.CouponDTO;
import org.shop.apiserver.application.dto.MemberCouponDTO;
import org.shop.apiserver.domain.model.coupon.Coupon;
import org.shop.apiserver.domain.model.coupon.MemberCoupon;
import org.shop.apiserver.domain.model.member.Member;
import org.shop.apiserver.infrastructure.persistence.jpa.CouponRepository;
import org.shop.apiserver.infrastructure.persistence.jpa.MemberCouponRepository;
import org.shop.apiserver.infrastructure.persistence.jpa.MemberRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * CouponService - 통합 구현
 * 동기 처리 방식으로 쿠폰 발급
 */
@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
@Primary
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;

    @Override
    public Long createCoupon(CouponDTO dto) {
        log.info("[CouponService] 쿠폰 생성 시작 - couponCode: {}", dto.getCouponCode());

        // stock 값을 maxIssueCount에 매핑
        int maxIssueCount = (dto.getStock() != null && dto.getStock() > 0) 
                ? dto.getStock().intValue() 
                : 0;

        Coupon coupon = Coupon.builder()
                .couponCode(dto.getCouponCode())
                .couponName(dto.getCouponName())
                .couponType(dto.getCouponType())
                .discountValue(dto.getDiscountValue())
                .minOrderAmount(dto.getMinOrderAmount())
                .endDate(dto.getEndDate())
                .maxIssueCount(maxIssueCount)
                .active(true)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        // 선착순 쿠폰인 경우 로그 출력
        if (maxIssueCount > 0) {
            log.info("[CouponService] 선착순 쿠폰 생성 완료 - couponId: {}, stock: {}", 
                    savedCoupon.getCouponId(), maxIssueCount);
        }

        return savedCoupon.getCouponId();
    }

    @Override
    public List<CouponDTO> getActiveCoupons() {
        return couponRepository.findByActiveTrue().stream()
                .filter(Coupon::isAvailable)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void issueCoupon(String email, String couponCode) {
        log.info("[CouponService] 쿠폰 발급 요청 - email: {}, couponCode: {}", 
                email, couponCode);

        try {
            // 1. 기본 유효성 검사
            Member member = memberRepository.findById(email)
                    .orElseThrow(() -> {
                        log.warn("[CouponService] 회원 없음 - email: {}", email);
                        return new NoSuchElementException("회원 없음");
                    });

            Coupon coupon = couponRepository.findByCouponCode(couponCode)
                    .orElseThrow(() -> {
                        log.warn("[CouponService] 쿠폰 없음 - couponCode: {}", couponCode);
                        return new NoSuchElementException("쿠폰 없음");
                    });

            // 2. 쿠폰 기본 유효성 검사
            if (!coupon.isAvailable()) {
                log.warn("[CouponService] 사용 불가 쿠폰 - couponCode: {}", couponCode);
                throw new IllegalStateException("사용 불가 쿠폰");
            }

            // 3. 이미 발급받은 쿠폰 확인
            if (memberCouponRepository.existsByMemberEmailAndCouponCouponCode(email, couponCode)) {
                log.warn("[CouponService] 이미 발급된 쿠폰 - email: {}, couponCode: {}", email, couponCode);
                throw new IllegalStateException("이미 발급된 쿠폰");
            }

            // 4. 쿠폰 발급 처리 (동기)
            MemberCoupon memberCoupon = MemberCoupon.builder()
                    .member(member)
                    .coupon(coupon)
                    .build();
            
            memberCouponRepository.save(memberCoupon);

            log.info("[CouponService] 쿠폰 발급 완료 - email: {}, couponCode: {}", email, couponCode);

        } catch (NoSuchElementException | IllegalStateException e) {
            log.error("[CouponService] 쿠폰 발급 실패 - email: {}, couponCode: {}, error: {}", 
                    email, couponCode, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[CouponService] 예외 발생 - email: {}, couponCode: {}, error: {}", 
                    email, couponCode, e.getMessage(), e);
            throw new RuntimeException("쿠폰 발급 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public List<MemberCouponDTO> getMyCoupons(String email) {
        return memberCouponRepository.findUsableCouponsByEmail(email).stream()
                .map(this::toMemberCouponDTO)
                .collect(Collectors.toList());
    }

    @Override
    public int useCoupon(Long memberCouponId, String email, int orderAmount) {
        log.info("[CouponService] 쿠폰 사용 - memberCouponId: {}, email: {}, orderAmount: {}", 
                memberCouponId, email, orderAmount);

        MemberCoupon mc = memberCouponRepository.findByIdAndEmail(memberCouponId, email)
                .orElseThrow(() -> {
                    log.warn("[CouponService] 쿠폰 없음 - memberCouponId: {}", memberCouponId);
                    return new NoSuchElementException("쿠폰 없음");
                });

        if (!mc.isUsable()) {
            log.warn("[CouponService] 사용 불가 쿠폰 - memberCouponId: {}", memberCouponId);
            throw new IllegalStateException("사용 불가 쿠폰");
        }

        int discount = mc.getCoupon().calculateDiscount(orderAmount);

        if (discount == 0) {
            log.warn("[CouponService] 최소 주문 금액 미달 - orderAmount: {}, minAmount: {}", 
                    orderAmount, mc.getCoupon().getMinOrderAmount());
            throw new IllegalStateException("최소 주문 금액 미달");
        }

        mc.use();
        log.info("[CouponService] 쿠폰 사용 완료 - memberCouponId: {}, discount: {}", 
                memberCouponId, discount);

        return discount;
    }

    // DTO 변환
    private CouponDTO toDTO(Coupon coupon) {
        return CouponDTO.builder()
                .couponId(coupon.getCouponId())
                .couponCode(coupon.getCouponCode())
                .couponName(coupon.getCouponName())
                .couponType(coupon.getCouponType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .endDate(coupon.getEndDate())
                .active(coupon.isActive())
                .build();
    }

    private MemberCouponDTO toMemberCouponDTO(MemberCoupon mc) {
        return MemberCouponDTO.builder()
                .memberCouponId(mc.getMemberCouponId())
                .coupon(toDTO(mc.getCoupon()))
                .used(mc.isUsed())
                .usable(mc.isUsable())
                .build();
    }
}
