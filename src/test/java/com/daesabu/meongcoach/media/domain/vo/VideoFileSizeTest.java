package com.daesabu.meongcoach.media.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VideoFileSizeTest {

	@Test
	void 허용_범위_안의_크기로_만들_수_있다() {
		assertThat(new VideoFileSize(1L).bytes()).isEqualTo(1L);
	}

	@Test
	void 상한과_정확히_같은_크기는_허용한다() {
		assertThatCode(() -> new VideoFileSize(VideoFileSize.MAX_BYTES)).doesNotThrowAnyException();
	}

	@Test
	void 상한을_1바이트라도_넘으면_생성에_실패한다() {
		assertThatThrownBy(() -> new VideoFileSize(VideoFileSize.MAX_BYTES + 1))
				.isInstanceOf(VideoFileSizeExceededException.class);
	}

	@ParameterizedTest
	@ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
	void 크기가_0_이하면_생성에_실패한다(long bytes) {
		assertThatThrownBy(() -> new VideoFileSize(bytes))
				.isInstanceOf(InvalidVideoFileSizeException.class);
	}

	@Test
	void 상한은_50MB다() {
		// 상한이 조용히 바뀌면 API 계약과 클라이언트 안내 문구가 어긋나므로 값 자체를 고정한다
		assertThat(VideoFileSize.MAX_BYTES).isEqualTo(52_428_800L);
	}

	@Test
	void 같은_값끼리는_동등하다() {
		assertThat(new VideoFileSize(1024L)).isEqualTo(new VideoFileSize(1024L));
	}
}
