package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.ai.domain.exception.AiVideoDurationOutOfRangeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AiVideoTest {

	@ParameterizedTest
	@ValueSource(longs = {10L, 300L})
	void 영상_길이가_10초_이상_300초_이하면_통과한다(long durationSeconds) {
		assertThatCode(() -> AiVideo.validateDuration(durationSeconds)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(longs = {9L, 301L})
	void 영상_길이가_범위를_벗어나면_예외를_던진다(long durationSeconds) {
		assertThatThrownBy(() -> AiVideo.validateDuration(durationSeconds))
				.isInstanceOf(AiVideoDurationOutOfRangeException.class);
	}
}
