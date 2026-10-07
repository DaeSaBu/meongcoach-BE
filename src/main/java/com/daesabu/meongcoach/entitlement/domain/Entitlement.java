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

	private Instant expiresAt;

	static Entitlement grant(Long userId, EntitlementType type, Instant expiresAt) {
		Entitlement entitlement = new Entitlement();

		entitlement.userId = requireNonNull(userId);
		entitlement.type = requireNonNull(type);
		entitlement.expiresAt = expiresAt;

		return entitlement;
	}

	void changeExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	void expire(Instant now) {
		state(isActive(now), "이미 만료된 이용권입니다");

		expiresAt = now;
	}

	public boolean isActive(Instant now) {
		return expiresAt == null || expiresAt.isAfter(now);
	}
}
