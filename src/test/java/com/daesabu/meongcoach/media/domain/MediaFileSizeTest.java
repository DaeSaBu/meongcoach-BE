package com.daesabu.meongcoach.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class MediaFileSizeTest {

	private static final long MAX_BYTES = 1_024L;

	@ParameterizedTest
	@ValueSource(longs = {1L, MAX_BYTES})
	void 상한_이하의_양수_크기는_만든다(long bytes) {
		MediaFileSize fileSize = new MediaFileSize(bytes, MAX_BYTES);

		assertThat(fileSize.bytes()).isEqualTo(bytes);
	}

	@Test
	void 상한을_넘으면_만들지_않는다() {
		assertThatThrownBy(() -> new MediaFileSize(MAX_BYTES + 1, MAX_BYTES))
				.isInstanceOf(VideoFileSizeExceededException.class);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(longs = {0L, -1L})
	void 크기가_없거나_0_이하면_만들지_않는다(Long bytes) {
		assertThatThrownBy(() -> new MediaFileSize(bytes, MAX_BYTES))
				.isInstanceOf(InvalidVideoFileSizeException.class);
	}
}
