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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;

    @Override
    public Long createCoupon(CouponDTO dto) {
        Coupon coupon = Coupon.builder()
                .couponCode(dto.getCouponCode())
                .couponName(dto.getCouponName())
                .couponType(dto.getCouponType())
                .discountValue(dto.getDiscountValue())
                .minOrderAmount(dto.getMinOrderAmount())
                .endDate(dto.getEndDate())
                .active(true)
                .build();

        return couponRepository.save(coupon).getCouponId();
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
        Member member = memberRepository.findById(email)
                .orElseThrow(() -> new NoSuchElementException("회원 없음"));

        Coupon coupon = couponRepository.findByCouponCode(couponCode)
                .orElseThrow(() -> new NoSuchElementException("쿠폰 없음"));

        if (!coupon.isAvailable()) {
            throw new IllegalStateException("사용 불가 쿠폰");
        }

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .member(member)
                .coupon(coupon)
                .used(false)
                .build();

        memberCouponRepository.save(memberCoupon);
    }

    @Override
    public List<MemberCouponDTO> getMyCoupons(String email) {
        return memberCouponRepository.findUsableCouponsByEmail(email).stream()
                .map(this::toMemberCouponDTO)
                .collect(Collectors.toList());
    }

    @Override
    public int useCoupon(Long memberCouponId, String email, int orderAmount) {
        MemberCoupon mc = memberCouponRepository.findByIdAndEmail(memberCouponId, email)
                .orElseThrow(() -> new NoSuchElementException("쿠폰 없음"));

        if (!mc.isUsable()) {
            throw new IllegalStateException("사용 불가 쿠폰");
        }

        int discount = mc.getCoupon().calculateDiscount(orderAmount);

        if (discount == 0) {
            throw new IllegalStateException("최소 주문 금액 미달");
        }

        mc.use();
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
