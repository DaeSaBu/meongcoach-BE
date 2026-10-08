package com.daesabu.meongcoach.promotion.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "promotion_code_redemptions",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_promotion_code_redemptions_promotion_code_id_user_id",
				columnNames = {"promotion_code_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionCodeRedemption extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "promotion_code_id", nullable = false)
	private PromotionCode promotionCode;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Instant accessEndsAt;

	static PromotionCodeRedemption create(PromotionCode promotionCode, Long userId, Instant accessEndsAt) {
		PromotionCodeRedemption redemption = new PromotionCodeRedemption();

		redemption.promotionCode = requireNonNull(promotionCode);
		redemption.userId = requireNonNull(userId);
		redemption.accessEndsAt = requireNonNull(accessEndsAt);

		return redemption;
	}
}
