package com.daesabu.meongcoach.user.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RefreshTokenIdTest {

	private static final String VALUE = "0f8fad5b-d9cb-469f-a165-70867728950e";

	@Test
	void 유효한_UUID_문자열로_생성에_성공한다() {
		RefreshTokenId tokenId = new RefreshTokenId(VALUE);

		assertThat(tokenId.value()).isEqualTo(VALUE);
	}

	@Test
	void 생성하면_매번_다른_유효한_값이_만들어진다() {
		RefreshTokenId first = RefreshTokenId.generate();
		RefreshTokenId second = RefreshTokenId.generate();

		assertThat(first).isNotEqualTo(second);
		assertThat(new RefreshTokenId(first.value())).isEqualTo(first);
	}

	@Test
	void 같은_값끼리는_동등하다() {
		assertThat(new RefreshTokenId(VALUE)).isEqualTo(new RefreshTokenId(VALUE));
	}

	@Test
	void 값이_null이면_생성에_실패한다() {
		assertThatThrownBy(() -> new RefreshTokenId(null))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "jti-1", "0F8FAD5B-D9CB-469F-A165-70867728950E",
			"0f8fad5b-d9cb-469f-a165-70867728950", "0f8fad5b-d9cb-469f-a165-70867728950ef",
			"0f8fad5bd9cb469fa16570867728950e"})
	void 값이_canonical_UUID_형식이_아니면_생성에_실패한다(String value) {
		assertThatThrownBy(() -> new RefreshTokenId(value))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	@Test
	void 검증_실패_예외에_값이_노출되지_않는다() {
		assertThatThrownBy(() -> new RefreshTokenId("jti-1"))
				.hasMessageNotContaining("jti-1");
	}
}
