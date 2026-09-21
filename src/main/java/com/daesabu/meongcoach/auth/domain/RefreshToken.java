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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발급한 리프레시 토큰의 기록. 토큰 원문 대신 JWT의 jti를 저장해 재발급 시 발급 이력이 있는 토큰인지 확인하고,
 * 탈퇴 등으로 더 이상 쓸 수 없게 된 토큰은 행을 지우지 않고 revokedAt으로 표시해 이력을 남긴다.
 */
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

	private RefreshToken(Long userId, RefreshTokenId tokenId, LocalDateTime expiresAt) {
		this.userId = userId;
		this.tokenId = tokenId;
		this.expiresAt = expiresAt;
	}

	public static RefreshToken issue(Long userId, RefreshTokenId tokenId, LocalDateTime expiresAt) {
		return new RefreshToken(userId, tokenId, expiresAt);
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
