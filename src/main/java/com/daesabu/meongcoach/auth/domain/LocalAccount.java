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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일반(이메일·비밀번호) 로그인 계정. 회원가입 API 없이 테스트 계정 시드로만 생성되며,
 * 가입·비밀번호 변경 API가 없으므로 생성 이후 수정되지 않는다.
 */
@Getter
@Entity
@Table(
		name = "local_accounts",
		uniqueConstraints = @UniqueConstraint(columnNames = "email")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalAccount extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	@Embedded
	private Email email;

	// 해싱은 시드 SQL에서, 대조는 PasswordMatcher가 맡는다 — 도메인은 해시된 값만 보관한다
	@Column(nullable = false, length = 255)
	private String passwordHash;

	private LocalAccount(Long userId, LocalAccountCreateCommand command) {
		this.userId = userId;
		this.email = command.email();
		this.passwordHash = command.passwordHash();
	}

	public static LocalAccount create(Long userId, LocalAccountCreateCommand command) {
		return new LocalAccount(userId, command);
	}

	public boolean isValidPassword(String password, PasswordMatcher matcher) {
		return matcher.matches(password, this.passwordHash);
	}
}
