package com.daesabu.meongcoach.auth.domain;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * 이메일 계정은 시드 SQL로만 만들어져 프로덕션 코드에 생성 경로가 없다. 테스트에서만 필드를 직접 채워 만든다.
 */
public final class EmailAccountFixture {

	private EmailAccountFixture() {
	}

	public static EmailAccount create(Long userId, Email email, String passwordHash) {
		EmailAccount account = new EmailAccount();
		ReflectionTestUtils.setField(account, "userId", userId);
		ReflectionTestUtils.setField(account, "email", email);
		ReflectionTestUtils.setField(account, "passwordHash", passwordHash);
		return account;
	}
}
