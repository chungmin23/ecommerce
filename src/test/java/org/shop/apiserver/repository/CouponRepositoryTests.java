package org.shop.apiserver.repository;

import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.shop.apiserver.domain.model.coupon.Coupon;
import org.shop.apiserver.domain.model.coupon.CouponType;
import org.shop.apiserver.domain.model.coupon.MemberCoupon;
import org.shop.apiserver.domain.model.member.Member;
import org.shop.apiserver.infrastructure.persistence.jpa.CouponRepository;
import org.shop.apiserver.infrastructure.persistence.jpa.MemberCouponRepository;
import org.shop.apiserver.infrastructure.persistence.jpa.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;

import java.time.LocalDateTime;

@SpringBootTest
@Log4j2
public class CouponRepositoryTests {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @Transactional
    @Commit
    public void testInsertCoupons() {

        // 5000원 할인 쿠폰
        Coupon coupon1 = Coupon.builder()
                .couponCode("WELCOME5000")
                .couponName("신규가입 5000원 할인")
                .couponType(CouponType.FIXED)
                .discountValue(5000)
                .minOrderAmount(30000)
                .endDate(LocalDateTime.now().plusMonths(1))
                .active(true)
                .build();
        couponRepository.save(coupon1);

        // 10% 할인 쿠폰
        Coupon coupon2 = Coupon.builder()
                .couponCode("SALE10")
                .couponName("10% 할인")
                .couponType(CouponType.PERCENT)
                .discountValue(10)
                .minOrderAmount(50000)
                .endDate(LocalDateTime.now().plusMonths(2))
                .active(true)
                .build();
        couponRepository.save(coupon2);

        log.info("쿠폰 생성 완료");
    }

    @Test
    @Transactional
    @Commit
    public void testIssueCoupon() {

        Member member = memberRepository.findById("user1@aaa.com").orElseThrow();
        Coupon coupon = couponRepository.findByCouponCode("WELCOME5000").orElseThrow();

        MemberCoupon mc = MemberCoupon.builder()
                .member(member)
                .coupon(coupon)
                .used(false)
                .build();

        memberCouponRepository.save(mc);
        log.info("쿠폰 발급 완료");
    }
}
