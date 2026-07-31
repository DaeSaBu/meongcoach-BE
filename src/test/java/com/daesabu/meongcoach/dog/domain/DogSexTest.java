package com.daesabu.meongcoach.dog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.domain.exception.InvalidDogSexException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DogSex 도메인")
class DogSexTest {

	@Test
	@DisplayName("문자열 코드를 성별로 변환한다")
	void fromConvertsCodeToDogSex() {
		assertThat(DogSex.from("MALE")).isEqualTo(DogSex.MALE);
	}

	@Test
	@DisplayName("잘못된 값이면 변환에 실패한다")
	void fromFailsWhenValueIsInvalid() {
		assertThatThrownBy(() -> DogSex.from("UNKNOWN"))
				.isInstanceOf(InvalidDogSexException.class);
	}

	@Test
	@DisplayName("null이면 변환에 실패한다")
	void fromFailsWhenValueIsNull() {
		assertThatThrownBy(() -> DogSex.from(null))
				.isInstanceOf(InvalidDogSexException.class);
	}
}
