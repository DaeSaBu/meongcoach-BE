package com.daesabu.meongcoach.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailAccountTest {

	private static final Long USER_ID = 1L;

	// 실제 BCrypt 대신 "hashed-" 접두어만 붙이는 대조기로 도메인이 대조 결과를 그대로 쓰는지만 검증한다
	private static final PasswordMatcher HASH_PREFIX_MATCHER = (raw, hash) -> hash.equals("hashed-" + raw);

	@Test
	void 대조기가_해시와_일치한다고_판단하면_유효한_비밀번호다() {
		EmailAccount account = account("hashed-secret");

		assertThat(account.isValidPassword("secret", HASH_PREFIX_MATCHER)).isTrue();
	}

	@Test
	void 대조기가_해시와_일치하지_않는다고_판단하면_유효하지_않은_비밀번호다() {
		EmailAccount account = account("hashed-secret");

		assertThat(account.isValidPassword("wrong", HASH_PREFIX_MATCHER)).isFalse();
	}

	private static EmailAccount account(String passwordHash) {
		Email email = new Email("test1@meongcoach.com");
		return EmailAccountFixture.create(USER_ID, email, passwordHash);
	}
}
