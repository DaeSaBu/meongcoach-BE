package com.daesabu.meongcoach.entitlement.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 가진 이용권 하나. 통합 상품을 사면 한 구매로 여러 행이 생긴다.
 * 이용권은 구매 외 경로로도 생길 수 있어 purchase 모듈의 구매와 별도 애그리거트로 두고 ID로만 참조한다.
 */
@Getter
@Entity
@Table(
		name = "entitlements",
		uniqueConstraints = @UniqueConstraint(columnNames = {"purchase_id", "type"}),
		// 회원이 특정 이용권을 가졌는지 확인한다. V7 마이그레이션과 이름·컬럼을 맞춘다
		indexes = @Index(name = "idx_entitlements_user_id_type", columnList = "user_id, type")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entitlement extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 다른 모듈이 거래 이력 없이 회원 기준으로 이용권만 조회하도록 구매와 별개로 둔다
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "purchase_id", nullable = false)
	private Long purchaseId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private EntitlementType type;

	// 회수되지 않은 이용권은 null
	private Instant revokedAt;

	// 구매 하나에서 이용권 묶음을 만드는 일은 Entitlements.grant()가 맡으므로 같은 패키지에서만 연다
	static Entitlement grant(Long userId, Long purchaseId, EntitlementType type) {
		Entitlement entitlement = new Entitlement();

		entitlement.userId = requireNonNull(userId);
		entitlement.purchaseId = requireNonNull(purchaseId);
		entitlement.type = requireNonNull(type);

		return entitlement;
	}
}
