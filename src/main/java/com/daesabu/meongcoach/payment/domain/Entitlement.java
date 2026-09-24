package com.daesabu.meongcoach.payment.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 구매로 얻은 시기별 이용권. 시기 하나당 한 행이라, 통합 이용권을 사면 같은 상품으로 시기 수만큼 행이 생긴다.
 */
@Getter
@Entity
@Table(name = "entitlements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entitlement extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 회원은 user 모듈의 애그리거트라 연관 대신 ID로만 참조한다
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LifeStage lifeStage;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ProductId productId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Store store;

	@Column(nullable = false)
	private Instant grantedAt;

	// 회수되지 않은 이용권은 null
	private Instant revokedAt;

	// 상품이 주는 시기만 넘어오도록 Entitlements.grant()가 시기를 골라 호출하므로 같은 패키지에서만 연다
	static Entitlement grant(LifeStage lifeStage, EntitlementGrantCommand command) {
		Entitlement entitlement = new Entitlement();

		entitlement.userId = Objects.requireNonNull(command.userId());
		entitlement.lifeStage = Objects.requireNonNull(lifeStage);
		entitlement.productId = Objects.requireNonNull(command.productId());
		entitlement.store = Objects.requireNonNull(command.store());
		entitlement.grantedAt = Objects.requireNonNull(command.grantedAt());

		return entitlement;
	}

	// 상품 단위 회수 규칙은 Entitlements.revoke()에 있으므로 application이 우회하지 못하게 같은 패키지에서만 연다
	void revoke(Instant revokedAt) {
		if (!isActive()) {
			return;
		}
		this.revokedAt = Objects.requireNonNull(revokedAt);
	}

	public boolean isActive() {
		return revokedAt == null;
	}
}
