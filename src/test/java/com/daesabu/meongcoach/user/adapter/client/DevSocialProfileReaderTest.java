package com.daesabu.meongcoach.user.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.exception.InvalidSocialTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("dev 소셜 프로필 리더")
class DevSocialProfileReaderTest {

	private static final String TOKEN = "dev-login-token-at-least-32-characters";

	@Test
	@DisplayName("설정한 토큰이면 고정 개발 계정을 반환한다")
	void readsFixedDevAccount() {
		DevSocialProfileReader reader = new DevSocialProfileReader(TOKEN);

		SocialAccountLinkCommand command = reader.read(TOKEN);

		assertThat(reader.provider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(command.provider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(command.providerId()).isEqualTo("dev-user");
	}

	@Test
	@DisplayName("설정과 다른 토큰을 거부한다")
	void rejectsInvalidToken() {
		DevSocialProfileReader reader = new DevSocialProfileReader(TOKEN);

		assertThatThrownBy(() -> reader.read("wrong-token"))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	@Test
	@DisplayName("설정 토큰이 32자보다 짧으면 기동을 거부한다")
	void rejectsShortConfiguredToken() {
		assertThatThrownBy(() -> new DevSocialProfileReader("too-short"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
