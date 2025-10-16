package org.shop.apiserver.application.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.dto.CouponDTO;
import org.shop.apiserver.application.dto.MemberCouponDTO;
import org.shop.apiserver.domain.event.CouponIssueEvent;
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

/**
 * Kafka를 이용한 비동기 쿠폰 발급 서비스
 * 성능 개선: 쿠폰 발급 요청을 즉시 응답하고 비동기로 처리
 */
@Service("couponServiceWithKafka")
@Transactional
@Log4j2
@RequiredArgsConstructor
public class CouponServiceWithKafkaImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;
    private final CouponKafkaProducer kafkaProducer;
    private final CouponIssueSagaService sagaService;

    @Override
    public Long createCoupon(CouponDTO dto) {
        log.info("[CouponServiceWithKafka] 쿠폰 생성 시작 - couponCode: {}", dto.getCouponCode());

        Coupon coupon = Coupon.builder()
                .couponCode(dto.getCouponCode())
                .couponName(dto.getCouponName())
                .couponType(dto.getCouponType())
                .discountValue(dto.getDiscountValue())
                .minOrderAmount(dto.getMinOrderAmount())
                .endDate(dto.getEndDate())
                .active(true)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);

        // 선착순 쿠폰인 경우 재고 초기화
        if (dto.getStock() != null && dto.getStock() > 0) {
            sagaService.initializeCouponStock(savedCoupon.getCouponId(), dto.getStock());
            log.info("[CouponServiceWithKafka] 쿠폰 선착순 재고 초기화 - couponId: {}, stock: {}",
                    savedCoupon.getCouponId(), dto.getStock());
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
        log.info("[CouponServiceWithKafka] 쿠폰 발급 요청 시작 - email: {}, couponCode: {}", email, couponCode);

        try {
            // 1. 기본 유효성 검사 (응답 속도를 위해 필수 검사만 수행)
            Member member = memberRepository.findById(email)
                    .orElseThrow(() -> {
                        log.warn("[CouponServiceWithKafka] 회원 없음 - email: {}", email);
                        return new NoSuchElementException("회원 없음");
                    });

            Coupon coupon = couponRepository.findByCouponCode(couponCode)
                    .orElseThrow(() -> {
                        log.warn("[CouponServiceWithKafka] 쿠폰 없음 - couponCode: {}", couponCode);
                        return new NoSuchElementException("쿠폰 없음");
                    });

            // 2. 쿠폰 기본 유효성 검사
            if (!coupon.isAvailable()) {
                log.warn("[CouponServiceWithKafka] 사용 불가 쿠폰 - couponCode: {}", couponCode);
                throw new IllegalStateException("사용 불가 쿠폰");
            }

            // 3. 이미 발급받은 쿠폰인지 빠르게 확인
            if (memberCouponRepository.existsByMemberEmailAndCouponCouponCode(email, couponCode)) {
                log.warn("[CouponServiceWithKafka] 이미 발급된 쿠폰 - email: {}, couponCode: {}", email, couponCode);
                throw new IllegalStateException("이미 발급된 쿠폰");
            }

            // 4. Kafka 이벤트 발행 (비동기 처리)
            CouponIssueEvent event = CouponIssueEvent.pending(email, couponCode, coupon.getCouponId());
            kafkaProducer.publishCouponIssueEvent(event);

            log.info("[CouponServiceWithKafka] 쿠폰 발급 요청 완료 (비동기 처리 중) - eventId: {}, email: {}, couponCode: {}",
                    event.getEventId(), email, couponCode);

        } catch (NoSuchElementException | IllegalStateException e) {
            log.error("[CouponServiceWithKafka] 쿠폰 발급 실패 - email: {}, couponCode: {}, error: {}",
                    email, couponCode, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[CouponServiceWithKafka] 예외 발생 - email: {}, couponCode: {}, error: {}",
                    email, couponCode, e.getMessage(), e);
            throw new RuntimeException("쿠폰 발급 요청 처리 중 오류가 발생했습니다", e);
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
        log.info("[CouponServiceWithKafka] 쿠폰 사용 - memberCouponId: {}, email: {}, orderAmount: {}",
                memberCouponId, email, orderAmount);

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
        log.info("[CouponServiceWithKafka] 쿠폰 사용 완료 - memberCouponId: {}, discount: {}", memberCouponId, discount);

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
                // 선착순 쿠폰의 경우 현재 재고 추가
                .stock(sagaService.getCouponStock(coupon.getCouponId()))
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
