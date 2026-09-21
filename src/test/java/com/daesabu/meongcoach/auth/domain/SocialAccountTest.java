package com.daesabu.meongcoach.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SocialAccountTest {

	private static final Long USER_ID = 1L;

	@Test
	void 연동하면_제공자_정보가_담긴_계정이_생성된다() {
		SocialAccount account = SocialAccount.link(USER_ID,
				new SocialAccountLinkCommand(SocialProvider.KAKAO, "kakao-123", new Email("test@kakao.com")));

		assertThat(account.getUserId()).isEqualTo(USER_ID);
		assertThat(account.getProvider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(account.getProviderId()).isEqualTo("kakao-123");
		assertThat(account.getEmail()).isEqualTo(new Email("test@kakao.com"));
	}

}
