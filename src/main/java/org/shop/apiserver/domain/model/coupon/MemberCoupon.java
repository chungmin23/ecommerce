package org.shop.apiserver.domain.model.coupon;

import jakarta.persistence.*;
import lombok.*;
import org.shop.apiserver.domain.model.member.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"member", "coupon"})
public class MemberCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberCouponId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_email", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Builder.Default
    private boolean used = false;  // 사용 여부

    private LocalDateTime usedAt;  // 사용 일시

    // 사용 처리
    public void use() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }

    // 사용 가능 여부
    public boolean isUsable() {
        return !used && coupon.isAvailable();
    }
}
