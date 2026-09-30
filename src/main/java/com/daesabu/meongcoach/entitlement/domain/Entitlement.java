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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원이 가진 이용권 하나. RevenueCat이 계산한 회원의 활성 이용권을 옮겨 둔 사본이며, 권한 확인은 RevenueCat 대신 이 행을 읽는다.
 * 회원·종류마다 한 행이라 환불로 회수된 뒤 다시 활성이 되면 새 행 대신 이 행을 되살린다.
 * 생성 시각은 구매 시각이 아니라 처음 동기화된 시각이다.
 */
@Getter
@Entity
@Table(
		name = "entitlements",
		// 회원이 특정 이용권을 가졌는지 확인하는 조회도 이 제약의 인덱스를 쓴다. V9 마이그레이션과 이름을 맞춘다
		uniqueConstraints = @UniqueConstraint(name = "uk_entitlements_user_id_type", columnNames = {"user_id", "type"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entitlement extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 회원은 user 모듈의 애그리거트라 연관 대신 ID로만 참조한다
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private EntitlementType type;

	// 회수되지 않은 이용권은 null. 처음 회수를 알게 된 시각을 남긴다
	private Instant revokedAt;

	// 부여·회수·복구는 회원의 이용권 전체를 보고 판단하는 Entitlements.synchronize()를 거치므로 같은 패키지에서만 연다
	static Entitlement grant(Long userId, EntitlementType type) {
		Entitlement entitlement = new Entitlement();

		entitlement.userId = requireNonNull(userId);
		entitlement.type = requireNonNull(type);

		return entitlement;
	}

	// 회수할 행은 Entitlements가 활성인 것만 고르므로, 이미 회수된 행이 오면 판단이 틀린 것이다
	void revoke(Instant now) {
		if (!isActive()) {
			throw new IllegalStateException("이미 회수된 이용권입니다: id=" + id);
		}
		revokedAt = requireNonNull(now);
	}

	// 복구할 행은 Entitlements가 회수된 것만 고르므로, 활성인 행이 오면 판단이 틀린 것이다
	void restore() {
		if (isActive()) {
			throw new IllegalStateException("이미 활성인 이용권입니다: id=" + id);
		}
		revokedAt = null;
	}

	public boolean isActive() {
		return revokedAt == null;
	}
}
