package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.exception.InvalidGenderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Gender 도메인")
class GenderTest {

	@Test
	@DisplayName("문자열 코드를 성별로 변환한다")
	void fromConvertsCodeToGender() {
		assertThat(Gender.from("FEMALE")).isEqualTo(Gender.FEMALE);
	}

	@Test
	@DisplayName("잘못된 값이면 변환에 실패한다")
	void fromFailsWhenValueIsInvalid() {
		assertThatThrownBy(() -> Gender.from("OTHER"))
				.isInstanceOf(InvalidGenderException.class);
	}

	@Test
	@DisplayName("null이면 변환에 실패한다")
	void fromFailsWhenValueIsNull() {
		assertThatThrownBy(() -> Gender.from(null))
				.isInstanceOf(InvalidGenderException.class);
	}
}
