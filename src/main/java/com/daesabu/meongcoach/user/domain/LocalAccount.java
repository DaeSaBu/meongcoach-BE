package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import com.daesabu.meongcoach.user.domain.command.LocalAccountCreateCommand;
import com.daesabu.meongcoach.user.domain.vo.Email;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일반(이메일·비밀번호) 로그인 계정. 회원가입 API 없이 테스트 계정 시드로만 생성되며,
 * 가입·비밀번호 변경 API가 없으므로 생성 이후 수정되지 않는다.
 */
@Getter
@Entity
@Table(name = "local_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalAccount extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Embedded
	@AttributeOverride(name = "address", column = @Column(name = "email", nullable = false, length = 255, unique = true))
	private Email email;

	// 해싱은 application 계층 책임 — 도메인은 해시된 값만 보관한다
	@Column(nullable = false, length = 255)
	private String passwordHash;

	private LocalAccount(User user, LocalAccountCreateCommand command) {
		this.user = user;
		this.email = command.email();
		this.passwordHash = command.passwordHash();
	}

	public static LocalAccount create(User user, LocalAccountCreateCommand command) {
		return new LocalAccount(user, command);
	}
}
