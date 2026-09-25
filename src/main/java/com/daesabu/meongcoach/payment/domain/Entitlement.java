package com.daesabu.meongcoach.payment.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 회원이 가진 권한 하나. 한 행이 RevenueCat entitlement 하나라, 통합 상품을 사면 한 구매로 여러 행이 생긴다.
 * 어느 권한을 줄지는 구매 시점 웹훅의 entitlement_ids를 그대로 따르므로, 대시보드 구성을 바꿔도 이미 부여한 권한에는 소급되지 않는다.
 * 권한은 구매 외 경로로도 생길 수 있어 {@link Purchase}와 별도 애그리거트로 두고 ID로만 참조한다.
 */
@Getter
@Entity
@Table(
		name = "entitlements",
		uniqueConstraints = @UniqueConstraint(columnNames = {"purchase_id", "identifier"}),
		// 회원이 특정 권한을 가졌는지 확인한다. V6 마이그레이션과 이름·컬럼을 맞춘다
		indexes = @Index(name = "idx_entitlements_user_id_identifier", columnList = "user_id, identifier")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entitlement extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 다른 모듈이 거래 이력 없이 회원 기준으로 권한만 조회하도록 구매와 별개로 둔다
	@Column(name = "user_id", nullable = false)
	private Long userId;

	// 이 권한을 준 구매. 별도 애그리거트라 연관 대신 ID로만 참조한다
	@Column(name = "purchase_id", nullable = false)
	private Long purchaseId;

	// RevenueCat entitlement 식별자(웹훅 entitlement_ids의 원소) 원본
	@Column(nullable = false, length = 50)
	private String identifier;

	// 회수되지 않은 권한은 null
	private Instant revokedAt;

	// 구매 하나에서 권한 묶음을 만드는 일은 Entitlements.grant()가 맡으므로 같은 패키지에서만 연다
	static Entitlement grant(Long userId, Long purchaseId, String identifier) {
		Entitlement entitlement = new Entitlement();

		entitlement.userId = requireNonNull(userId);
		entitlement.purchaseId = requireNonNull(purchaseId);
		entitlement.identifier = requireNonNull(identifier);

		return entitlement;
	}
}
