package com.daesabu.meongcoach.community.adapter.webapi.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.community.application.provided.dto.AuthorResult;
import org.junit.jupiter.api.Test;

class AuthorResponseTest {

	private static final String PROFILE_IMAGE_URL = "https://media.meongcoach.com/images/dog-profile/7/choco.jpg";

	@Test
	void 일반_회원은_강아지_이름에_보호자를_붙인다() {
		AuthorResponse response = AuthorResponse.from(new AuthorResult(7L, "초코", PROFILE_IMAGE_URL, false));

		assertThat(response.nickname()).isEqualTo("초코 보호자");
	}

	@Test
	void 전문가는_강아지_이름을_그대로_쓴다() {
		AuthorResponse response = AuthorResponse.from(new AuthorResult(7L, "멍코치 훈련사", PROFILE_IMAGE_URL, true));

		assertThat(response.nickname()).isEqualTo("멍코치 훈련사");
	}
}
