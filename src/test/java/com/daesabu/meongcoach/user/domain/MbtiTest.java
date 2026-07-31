package com.daesabu.meongcoach.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.user.domain.exception.InvalidMbtiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Mbti 도메인")
class MbtiTest {

	@Test
	@DisplayName("문자열 코드를 MBTI로 변환한다")
	void fromConvertsCodeToMbti() {
		assertThat(Mbti.from("INTJ")).isEqualTo(Mbti.INTJ);
	}

	@Test
	@DisplayName("잘못된 값이면 변환에 실패한다")
	void fromFailsWhenValueIsInvalid() {
		assertThatThrownBy(() -> Mbti.from("XXXX"))
				.isInstanceOf(InvalidMbtiException.class);
	}

	@Test
	@DisplayName("null이면 변환에 실패한다")
	void fromFailsWhenValueIsNull() {
		assertThatThrownBy(() -> Mbti.from(null))
				.isInstanceOf(InvalidMbtiException.class);
	}
}
