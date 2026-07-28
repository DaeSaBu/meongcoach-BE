package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.exception.UnsupportedSocialProviderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SocialProvider 도메인")
class SocialProviderTest {

	@ParameterizedTest
	@DisplayName("대소문자와 무관하게 제공자로 변환된다")
	@ValueSource(strings = {"kakao", "KAKAO", "Kakao"})
	void fromConvertsIgnoringCase(String value) {
		assertThat(SocialProvider.from(value)).isEqualTo(SocialProvider.KAKAO);
	}

	@ParameterizedTest
	@DisplayName("지원하지 않는 제공자면 변환에 실패한다")
	@ValueSource(strings = {"naver", "facebook", "  "})
	@NullAndEmptySource
	void fromFailsWhenProviderIsUnsupported(String value) {
		assertThatThrownBy(() -> SocialProvider.from(value))
				.isInstanceOf(UnsupportedSocialProviderException.class);
	}

	@Test
	@DisplayName("정의된 모든 제공자를 변환할 수 있다")
	void fromConvertsEveryDefinedProvider() {
		assertThat(SocialProvider.from("google")).isEqualTo(SocialProvider.GOOGLE);
		assertThat(SocialProvider.from("apple")).isEqualTo(SocialProvider.APPLE);
	}
}
