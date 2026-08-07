package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.domain.command.SocialAccountLinkCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SocialAccount 도메인")
class SocialAccountTest {

	@Test
	@DisplayName("연동하면 제공자 정보가 담긴 계정이 생성된다")
	void linkCreatesAccountWithProviderInfo() {
		User user = User.registerMember();

		SocialAccount account = SocialAccount.link(user,
				new SocialAccountLinkCommand(SocialProvider.KAKAO, "kakao-123", "test@kakao.com"));

		assertThat(account.getUser()).isEqualTo(user);
		assertThat(account.getProvider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(account.getProviderId()).isEqualTo("kakao-123");
		assertThat(account.getEmail()).isEqualTo("test@kakao.com");
	}
}
