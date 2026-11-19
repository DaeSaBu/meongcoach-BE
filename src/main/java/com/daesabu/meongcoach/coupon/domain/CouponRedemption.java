package com.daesabu.meongcoach.coupon.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "coupon_redemptions",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_coupon_redemptions_coupon_id_user_id", columnNames = {"coupon_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponRedemption extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long couponId;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private int aiTrialCount;

	static CouponRedemption register(Long couponId, Long userId, int aiTrialCount) {
		CouponRedemption redemption = new CouponRedemption();

		redemption.couponId = requireNonNull(couponId);
		redemption.userId = requireNonNull(userId);
		redemption.aiTrialCount = aiTrialCount;

		return redemption;
	}
}
