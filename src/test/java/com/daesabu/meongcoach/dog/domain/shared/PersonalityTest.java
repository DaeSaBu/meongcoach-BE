package com.daesabu.meongcoach.dog.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.domain.exception.InvalidPersonalityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Personality 도메인")
class PersonalityTest {

	@Test
	@DisplayName("문자열 코드를 성격으로 변환한다")
	void fromConvertsCodeToPersonality() {
		assertThat(Personality.from("TIMID")).isEqualTo(Personality.TIMID);
	}

	@Test
	@DisplayName("확장된 성격 코드도 변환한다")
	void fromConvertsExpandedCodeToPersonality() {
		assertThat(Personality.from("FEARFUL")).isEqualTo(Personality.FEARFUL);
	}

	@Test
	@DisplayName("잘못된 값이면 변환에 실패한다")
	void fromFailsWhenValueIsInvalid() {
		assertThatThrownBy(() -> Personality.from("BRAVE"))
				.isInstanceOf(InvalidPersonalityException.class);
	}

	@Test
	@DisplayName("null이면 변환에 실패한다")
	void fromFailsWhenValueIsNull() {
		assertThatThrownBy(() -> Personality.from(null))
				.isInstanceOf(InvalidPersonalityException.class);
	}
}
