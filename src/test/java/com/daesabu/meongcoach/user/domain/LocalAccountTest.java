package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.domain.command.LocalAccountCreateCommand;
import com.daesabu.meongcoach.user.domain.vo.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LocalAccount 도메인")
class LocalAccountTest {

	@Test
	@DisplayName("생성하면 자격 증명이 담긴 계정이 초기화된다")
	void createInitializesAccountWithCredentials() {
		User user = User.registerOnboardingMember();
		Email email = new Email("test1@meongcoach.com");

		LocalAccount account = LocalAccount.create(user, new LocalAccountCreateCommand(email, "hashed-password"));

		assertThat(account.getUser()).isEqualTo(user);
		assertThat(account.getEmail()).isEqualTo(email);
		assertThat(account.getPasswordHash()).isEqualTo("hashed-password");
	}
}
