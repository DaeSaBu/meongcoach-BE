package com.daesabu.meongcoach.coupon.domain;

import com.daesabu.meongcoach.coupon.domain.exception.CouponExpiredException;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "coupons",
		uniqueConstraints = @UniqueConstraint(name = "uk_coupons_code", columnNames = "code")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

	public static final int CODE_MAX_LENGTH = 50;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = CODE_MAX_LENGTH)
	private String code;

	@Column(nullable = false)
	private int aiTrialCount;

	private Instant redeemableUntil;

	public static String normalizeCode(String code) {
		return code.strip().toUpperCase(Locale.ROOT);
	}

	public CouponRedemption redeem(Long userId, Instant now) {
		if (isExpired(now)) {
			throw new CouponExpiredException();
		}

		return CouponRedemption.register(id, userId, aiTrialCount);
	}

	private boolean isExpired(Instant now) {
		return redeemableUntil != null && !now.isBefore(redeemableUntil);
	}
}
