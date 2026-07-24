package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.domain.vo.Email;
import org.junit.jupiter.api.Test;

class LocalAccountTest {

	@Test
	void createInitializesAccountWithCredentials() {
		User user = User.registerMember();
		Email email = new Email("test1@meongcoach.com");

		LocalAccount account = LocalAccount.create(user, email, "hashed-password");

		assertThat(account.getUser()).isEqualTo(user);
		assertThat(account.getEmail()).isEqualTo(email);
		assertThat(account.getPasswordHash()).isEqualTo("hashed-password");
	}
}
