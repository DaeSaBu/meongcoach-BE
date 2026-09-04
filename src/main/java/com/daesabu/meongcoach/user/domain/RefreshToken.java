package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import com.daesabu.meongcoach.user.domain.vo.RefreshTokenId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// JWT jti. 토큰 원문은 저장하지 않는다
	@Embedded
	@AttributeOverride(name = "value", column = @Column(name = "token_id", nullable = false, length = 36))
	private RefreshTokenId tokenId;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	// 무효화되지 않은 토큰은 null
	private LocalDateTime revokedAt;

	private RefreshToken(User user, RefreshTokenId tokenId, LocalDateTime expiresAt) {
		this.user = user;
		this.tokenId = tokenId;
		this.expiresAt = expiresAt;
	}

	public static RefreshToken issue(User user, RefreshTokenId tokenId, LocalDateTime expiresAt) {
		return new RefreshToken(user, tokenId, expiresAt);
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
