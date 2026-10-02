package com.daesabu.meongcoach.entitlement.domain;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

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

@Getter
@Entity
@Table(
		name = "entitlements",
		uniqueConstraints = @UniqueConstraint(name = "uk_entitlements_user_id_type", columnNames = {"user_id", "type"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entitlement extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private EntitlementType type;

	private Instant revokedAt;

	static Entitlement grant(Long userId, EntitlementType type) {
		Entitlement entitlement = new Entitlement();

		entitlement.userId = requireNonNull(userId);
		entitlement.type = requireNonNull(type);

		return entitlement;
	}

	void revoke() {
		state(isActive(), "이미 회수된 이용권입니다");

		revokedAt = Instant.now();
	}

	void restore() {
		state(!isActive(), "이미 활성인 이용권입니다");

		revokedAt = null;
	}

	public boolean isActive() {
		return revokedAt == null;
	}
}
