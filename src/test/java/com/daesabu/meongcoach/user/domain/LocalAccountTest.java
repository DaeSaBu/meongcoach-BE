package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.domain.command.LocalAccountCreateCommand;
import com.daesabu.meongcoach.user.domain.vo.Email;
import org.junit.jupiter.api.Test;

class LocalAccountTest {

	@Test
	void 생성하면_자격_증명이_담긴_계정이_초기화된다() {
		User user = User.registerOnboardingMember();
		Email email = new Email("test1@meongcoach.com");

		LocalAccount account = LocalAccount.create(user, new LocalAccountCreateCommand(email, "hashed-password"));

		assertThat(account.getUser()).isEqualTo(user);
		assertThat(account.getEmail()).isEqualTo(email);
		assertThat(account.getPasswordHash()).isEqualTo("hashed-password");
	}
}
