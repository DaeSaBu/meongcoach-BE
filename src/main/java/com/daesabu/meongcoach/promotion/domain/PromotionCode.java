package com.daesabu.meongcoach.promotion.domain;

import static jakarta.persistence.EnumType.STRING;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.promotion.domain.exception.PromotionCodeExhaustedException;
import com.daesabu.meongcoach.promotion.domain.exception.PromotionCodeExpiredException;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "promotion_codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionCode extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String code;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
			name = "promotion_code_entitlement_types",
			joinColumns = @JoinColumn(name = "promotion_code_id"),
			uniqueConstraints = @UniqueConstraint(columnNames = {"promotion_code_id", "entitlement_type"}))
	@Enumerated(STRING)
	@Column(name = "entitlement_type", nullable = false, length = 50)
	private Set<EntitlementType> entitlementTypes = new HashSet<>();

	@Embedded
	private AccessTerm accessTerm;

	private Instant redeemableUntil;

	private Integer maxRedemptions;

	public PromotionCodeRedemption redeem(Long userId, long redeemedCount, Instant now) {
		Instant accessEndsAt = accessTerm.endsAtFrom(now);
		if (isRedemptionClosed(now) || !accessEndsAt.isAfter(now)) {
			throw new PromotionCodeExpiredException();
		}
		if (isExhausted(redeemedCount)) {
			throw new PromotionCodeExhaustedException();
		}
		return PromotionCodeRedemption.create(this, userId, accessEndsAt);
	}

	private boolean isRedemptionClosed(Instant now) {
		return redeemableUntil != null && !now.isBefore(redeemableUntil);
	}

	private boolean isExhausted(long redeemedCount) {
		return maxRedemptions != null && redeemedCount >= maxRedemptions;
	}
}
