package com.daesabu.meongcoach.user.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.exception.InvalidEmailException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

	@Test
	void 유효한_주소로_생성에_성공한다() {
		Email email = new Email("test@kakao.com");

		assertThat(email.address()).isEqualTo("test@kakao.com");
	}

	@Test
	void 같은_주소끼리는_동등하다() {
		assertThat(new Email("test@kakao.com")).isEqualTo(new Email("test@kakao.com"));
	}

	@Test
	void 주소가_null이면_생성에_실패한다() {
		assertThatThrownBy(() -> new Email(null))
				.isInstanceOf(InvalidEmailException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "invalid", "no-at-sign.com", "@no-local.com", "user@", "user@domain",
			"user@domain."})
	void 주소_형식이_잘못되면_생성에_실패한다(String address) {
		assertThatThrownBy(() -> new Email(address))
				.isInstanceOf(InvalidEmailException.class);
	}

	@Test
	void 주소가_최대_길이를_초과하면_생성에_실패한다() {
		String longAddress = "a".repeat(250) + "@test.com";

		assertThatThrownBy(() -> new Email(longAddress))
				.isInstanceOf(InvalidEmailException.class);
	}
}
