package com.daesabu.meongcoach.auth.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "refresh_tokens",
		uniqueConstraints = @UniqueConstraint(columnNames = "token_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 회원은 user 모듈의 애그리거트라 연관 대신 ID로만 참조한다
	@Column(name = "user_id", nullable = false)
	private Long userId;

	// JWT jti. 토큰 원문은 저장하지 않는다
	@Embedded
	private RefreshTokenId tokenId;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	// 무효화되지 않은 토큰은 null
	private LocalDateTime revokedAt;

	public static RefreshToken register(Long userId, RefreshTokenRegisterCommand command) {
		RefreshToken refreshToken = new RefreshToken();

		refreshToken.userId = Objects.requireNonNull(userId);
		refreshToken.tokenId = command.tokenId();
		refreshToken.expiresAt = Objects.requireNonNull(command.expiresAt());

		return refreshToken;
	}

	public void revoke() {
		if (isRevoked()) {
			return;
		}
		this.revokedAt = LocalDateTime.now();
	}

	public boolean isUsable(LocalDateTime now) {
		if (isRevoked()) {
			return false;
		}
		return now.isBefore(expiresAt);
	}

	private boolean isRevoked() {
		return revokedAt != null;
	}
}
