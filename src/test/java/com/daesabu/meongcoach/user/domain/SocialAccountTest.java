package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.user.domain.command.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.shared.Email;
import org.junit.jupiter.api.Test;

class SocialAccountTest {

	@Test
	void 연동하면_제공자_정보가_담긴_계정이_생성된다() {
		User user = User.registerOnboardingMember();

		SocialAccount account = SocialAccount.link(user,
				new SocialAccountLinkCommand(SocialProvider.KAKAO, "kakao-123", new Email("test@kakao.com")));

		assertThat(account.getUser()).isEqualTo(user);
		assertThat(account.getProvider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(account.getProviderId()).isEqualTo("kakao-123");
		assertThat(account.getEmail()).isEqualTo(new Email("test@kakao.com"));
	}

	@Test
	void 제공자가_이메일을_주지_않으면_이메일_없이_연결한다() {
		User user = User.registerOnboardingMember();

		SocialAccount account = SocialAccount.link(user,
				new SocialAccountLinkCommand(SocialProvider.KAKAO, "kakao-123", null));

		assertThat(account.getEmail()).isNull();
	}
}
