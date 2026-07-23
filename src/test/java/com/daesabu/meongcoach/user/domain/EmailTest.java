package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

	@Test
	void createSucceedsWithValidAddress() {
		Email email = new Email("test@kakao.com");

		assertThat(email.address()).isEqualTo("test@kakao.com");
	}

	@Test
	void sameAddressesAreEqual() {
		assertThat(new Email("test@kakao.com")).isEqualTo(new Email("test@kakao.com"));
	}

	@Test
	void createFailsWhenAddressIsNull() {
		assertThatThrownBy(() -> new Email(null))
				.isInstanceOf(InvalidEmailException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "invalid", "no-at-sign.com", "@no-local.com", "user@", "user@domain",
			"user@domain."})
	void createFailsWhenAddressFormatIsInvalid(String address) {
		assertThatThrownBy(() -> new Email(address))
				.isInstanceOf(InvalidEmailException.class);
	}

	@Test
	void createFailsWhenAddressExceedsMaxLength() {
		String longAddress = "a".repeat(250) + "@test.com";

		assertThatThrownBy(() -> new Email(longAddress))
				.isInstanceOf(InvalidEmailException.class);
	}
}
