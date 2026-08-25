package com.daesabu.meongcoach.dog.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.domain.exception.InvalidBreedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Breed 도메인")
class BreedTest {

	@Test
	@DisplayName("문자열 코드를 견종으로 변환한다")
	void fromConvertsCodeToBreed() {
		assertThat(Breed.from("POODLE")).isEqualTo(Breed.POODLE);
	}

	@Test
	@DisplayName("잘못된 값이면 변환에 실패한다")
	void fromFailsWhenValueIsInvalid() {
		assertThatThrownBy(() -> Breed.from("UNKNOWN"))
				.isInstanceOf(InvalidBreedException.class);
	}

	@Test
	@DisplayName("null이면 변환에 실패한다")
	void fromFailsWhenValueIsNull() {
		assertThatThrownBy(() -> Breed.from(null))
				.isInstanceOf(InvalidBreedException.class);
	}
}
