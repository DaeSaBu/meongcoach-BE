package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SocialAccountTest {

	@Test
	void linkCreatesAccountWithProviderInfo() {
		User user = User.registerMember();

		SocialAccount account = SocialAccount.link(user, SocialProvider.KAKAO, "kakao-123", "test@kakao.com");

		assertThat(account.getUser()).isEqualTo(user);
		assertThat(account.getProvider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(account.getProviderId()).isEqualTo("kakao-123");
		assertThat(account.getEmail()).isEqualTo("test@kakao.com");
	}
}
