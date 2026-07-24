package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 토큰 검증 시 User 객체 탐색이 불필요하고 대량 발급·삭제되는 테이블이라 ID로만 참조한다
	@Column(nullable = false)
	private Long userId;

	// 원문 저장 금지, 해시만 저장한다
	@Column(nullable = false, length = 255)
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private RefreshTokenStatus status;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	public static RefreshToken issue(Long userId, String tokenHash, LocalDateTime expiresAt) {
		RefreshToken token = new RefreshToken();
		token.userId = userId;
		token.tokenHash = tokenHash;
		token.status = RefreshTokenStatus.ACTIVE;
		token.expiresAt = expiresAt;
		return token;
	}

	public void revoke() {
		this.status = RefreshTokenStatus.REVOKED;
	}

	public boolean isExpired(LocalDateTime now) {
		return now.isAfter(expiresAt);
	}
}
