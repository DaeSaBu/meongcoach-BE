package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.domain.command.LocalAccountCreateCommand;
import com.daesabu.meongcoach.user.domain.shared.Email;
import org.junit.jupiter.api.Test;

class LocalAccountTest {

	@Test
	void 생성하면_자격_증명이_담긴_계정이_초기화된다() {
		User user = User.registerUser();
		Email email = new Email("test1@meongcoach.com");

		LocalAccount account = LocalAccount.create(user, new LocalAccountCreateCommand(email, "hashed-password"));

		assertThat(account.getUser()).isEqualTo(user);
		assertThat(account.getEmail()).isEqualTo(email);
		assertThat(account.getPasswordHash()).isEqualTo("hashed-password");
	}

	@Test
	void 대조기가_해시와_일치한다고_판단하면_유효한_비밀번호다() {
		LocalAccount account = account("hashed-secret");

		assertThat(account.isValidPassword("secret", HASH_PREFIX_MATCHER)).isTrue();
	}

	@Test
	void 대조기가_해시와_일치하지_않는다고_판단하면_유효하지_않은_비밀번호다() {
		LocalAccount account = account("hashed-secret");

		assertThat(account.isValidPassword("wrong", HASH_PREFIX_MATCHER)).isFalse();
	}

	// 실제 BCrypt 대신 "hashed-" 접두어만 붙이는 대조기로 도메인이 대조 결과를 그대로 쓰는지만 검증한다
	private static final PasswordMatcher HASH_PREFIX_MATCHER = (raw, hash) -> hash.equals("hashed-" + raw);

	private static LocalAccount account(String passwordHash) {
		User user = User.registerUser();
		Email email = new Email("test1@meongcoach.com");
		return LocalAccount.create(user, new LocalAccountCreateCommand(email, passwordHash));
	}
}
