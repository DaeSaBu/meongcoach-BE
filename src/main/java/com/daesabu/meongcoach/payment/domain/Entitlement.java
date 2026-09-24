package com.daesabu.meongcoach.payment.domain;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 구매로 얻은 시기별 이용권. 시기 하나당 한 행이라, 통합 이용권을 사면 같은 거래로 시기 수만큼 행이 생긴다.
 * 바뀌는 값은 회수 시각뿐이라 수정 시각 없이 생성 시각만 기록한다.
 */
@Getter
@Entity
@Table(
		name = "entitlements",
		uniqueConstraints = @UniqueConstraint(columnNames = {"transaction_id", "life_stage"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entitlement extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 회원은 user 모듈의 애그리거트라 연관 대신 ID로만 참조한다
	@Column(name = "user_id", nullable = false)
	private Long userId;

	// 스토어 거래 ID(웹훅 transaction_id). 부여·회수를 거래 단위로 해서 웹훅 재전송과 환불 후 재구매를 구분한다
	@Column(name = "transaction_id", nullable = false, length = 100)
	private String transactionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "life_stage", nullable = false, length = 20)
	private LifeStage lifeStage;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ProductId productId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Store store;

	// 스토어에서 구매한 시각(웹훅 purchased_at_ms). 행을 기록한 시각은 createdAt이다
	@Column(nullable = false)
	private Instant purchasedAt;

	// 회수되지 않은 이용권은 null
	private Instant revokedAt;

	// 상품이 주는 시기만 넘어오도록 Entitlements.grant()가 시기를 골라 호출하므로 같은 패키지에서만 연다
	static Entitlement grant(LifeStage lifeStage, EntitlementGrantCommand command) {
		Entitlement entitlement = new Entitlement();

		entitlement.userId = Objects.requireNonNull(command.userId());
		entitlement.transactionId = Objects.requireNonNull(command.transactionId());
		entitlement.lifeStage = Objects.requireNonNull(lifeStage);
		entitlement.productId = Objects.requireNonNull(command.productId());
		entitlement.store = Objects.requireNonNull(command.store());
		entitlement.purchasedAt = Objects.requireNonNull(command.purchasedAt());

		return entitlement;
	}

	// 거래 단위 회수 규칙은 Entitlements.revoke()에 있으므로 application이 우회하지 못하게 같은 패키지에서만 연다
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
