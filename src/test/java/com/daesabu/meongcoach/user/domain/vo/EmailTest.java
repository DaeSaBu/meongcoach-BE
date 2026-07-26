package com.daesabu.meongcoach.user.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.exception.InvalidEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Email 값 객체")
class EmailTest {

	@Test
	@DisplayName("유효한 주소로 생성에 성공한다")
	void createSucceedsWithValidAddress() {
		Email email = new Email("test@kakao.com");

		assertThat(email.address()).isEqualTo("test@kakao.com");
	}

	@Test
	@DisplayName("같은 주소끼리는 동등하다")
	void sameAddressesAreEqual() {
		assertThat(new Email("test@kakao.com")).isEqualTo(new Email("test@kakao.com"));
	}

	@Test
	@DisplayName("주소가 null이면 생성에 실패한다")
	void createFailsWhenAddressIsNull() {
		assertThatThrownBy(() -> new Email(null))
				.isInstanceOf(InvalidEmailException.class);
	}

	@ParameterizedTest
	@DisplayName("주소 형식이 잘못되면 생성에 실패한다")
	@ValueSource(strings = {"", " ", "invalid", "no-at-sign.com", "@no-local.com", "user@", "user@domain",
			"user@domain."})
	void createFailsWhenAddressFormatIsInvalid(String address) {
		assertThatThrownBy(() -> new Email(address))
				.isInstanceOf(InvalidEmailException.class);
	}

	@Test
	@DisplayName("주소가 최대 길이를 초과하면 생성에 실패한다")
	void createFailsWhenAddressExceedsMaxLength() {
		String longAddress = "a".repeat(250) + "@test.com";

		assertThatThrownBy(() -> new Email(longAddress))
				.isInstanceOf(InvalidEmailException.class);
	}
}
