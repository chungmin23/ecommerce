package org.shop.apiserver.infrastructure.persistence.jpa;

import org.shop.apiserver.domain.model.coupon.CouponIssueSaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 쿠폰 발급 SAGA 이벤트 저장소
 */
@Repository
public interface CouponIssueSagaRepository extends JpaRepository<CouponIssueSaga, Long> {

    /**
     * eventId로 SAGA 조회
     */
    Optional<CouponIssueSaga> findByEventId(String eventId);

    /**
     * 회원 이메일로 쿠폰 발급 이벤트 조회
     */
    List<CouponIssueSaga> findByMemberEmailOrderByTimestampDesc(String memberEmail);

    /**
     * 쿠폰 코드로 발급 이벤트 조회
     */
    List<CouponIssueSaga> findByCouponCodeOrderByTimestampDesc(String couponCode);

    /**
     * 상태별로 SAGA 조회
     */
    List<CouponIssueSaga> findByStatusOrderByTimestampDesc(String status);

    /**
     * 실패한 이벤트 중 재시도 가능한 것 조회
     */
    @Query("SELECT s FROM CouponIssueSaga s WHERE s.status = 'FAILED' AND s.retryCount < 3 " +
           "ORDER BY s.timestamp ASC")
    List<CouponIssueSaga> findFailedEventsForRetry();

    /**
     * 특정 시간 이후 생성된 SAGA 이벤트 조회
     */
    List<CouponIssueSaga> findByCreatedAtAfterOrderByTimestampDesc(LocalDateTime createdAt);

    /**
     * 회원과 쿠폰으로 성공한 발급 이벤트 조회
     */
    @Query("SELECT s FROM CouponIssueSaga s WHERE s.memberEmail = :memberEmail " +
           "AND s.couponCode = :couponCode AND s.status = 'SUCCESS'")
    Optional<CouponIssueSaga> findSuccessfulIssuance(@Param("memberEmail") String memberEmail,
                                                      @Param("couponCode") String couponCode);
}
