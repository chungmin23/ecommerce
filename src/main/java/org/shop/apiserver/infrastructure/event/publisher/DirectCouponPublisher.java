package org.shop.apiserver.infrastructure.event.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.port.CouponPublisher;
import org.shop.apiserver.domain.event.CouponIssueEvent;
import org.shop.apiserver.domain.model.coupon.Coupon;
import org.shop.apiserver.domain.model.coupon.MemberCoupon;
import org.shop.apiserver.domain.model.member.Member;
import org.shop.apiserver.infrastructure.persistence.jpa.CouponRepository;
import org.shop.apiserver.infrastructure.persistence.jpa.MemberCouponRepository;
import org.shop.apiserver.infrastructure.persistence.jpa.MemberRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 동기 방식 쿠폰 발급 구현 (기본값)
 * application.yml에서 coupon.issue.async=false일 때 또는 설정이 없을 때 활성화
 */
@Component
@ConditionalOnProperty(name = "coupon.issue.async", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
@Log4j2
public class DirectCouponPublisher implements CouponPublisher {

    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public void publish(CouponIssueEvent event) {
        log.info("[DirectCouponPublisher] 쿠폰 발급 이벤트 처리 (동기) - eventId: {}, email: {}", 
                event.getEventId(), event.getMemberEmail());

        try {
            Member member = memberRepository.findById(event.getMemberEmail())
                    .orElseThrow(() -> new RuntimeException("회원 없음"));

            Coupon coupon = couponRepository.findByCouponCode(event.getCouponCode())
                    .orElseThrow(() -> new RuntimeException("쿠폰 없음"));

            MemberCoupon memberCoupon = MemberCoupon.builder()
                    .member(member)
                    .coupon(coupon)
                    .used(false)
                    .build();

            memberCouponRepository.save(memberCoupon);
            log.info("[DirectCouponPublisher] 쿠폰 발급 완료 - email: {}", event.getMemberEmail());

        } catch (Exception e) {
            log.error("[DirectCouponPublisher] 쿠폰 발급 실패 - eventId: {}, error: {}", 
                    event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("쿠폰 발급 처리 중 오류가 발생했습니다", e);
        }
    }
}
