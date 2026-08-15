package com.daesabu.meongcoach.media.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("영상 파일 크기 값 객체")
class VideoFileSizeTest {

	@Test
	@DisplayName("허용 범위 안의 크기로 만들 수 있다")
	void createSucceedsWithValidSize() {
		assertThat(new VideoFileSize(1L).bytes()).isEqualTo(1L);
	}

	@Test
	@DisplayName("상한과 정확히 같은 크기는 허용한다")
	void createSucceedsAtMaxBytes() {
		assertThatCode(() -> new VideoFileSize(VideoFileSize.MAX_BYTES)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("상한을 1바이트라도 넘으면 생성에 실패한다")
	void createFailsWhenExceedsMaxBytes() {
		assertThatThrownBy(() -> new VideoFileSize(VideoFileSize.MAX_BYTES + 1))
				.isInstanceOf(VideoFileSizeExceededException.class);
	}

	@ParameterizedTest
	@ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
	@DisplayName("0 이하의 크기는 생성에 실패한다")
	void createFailsWhenNotPositive(long bytes) {
		assertThatThrownBy(() -> new VideoFileSize(bytes))
				.isInstanceOf(InvalidVideoFileSizeException.class);
	}

	@Test
	@DisplayName("상한은 50MB다")
	void maxBytesIs50Megabytes() {
		// 상한이 조용히 바뀌면 API 계약과 클라이언트 안내 문구가 어긋나므로 값 자체를 고정한다
		assertThat(VideoFileSize.MAX_BYTES).isEqualTo(52_428_800L);
	}

	@Test
	@DisplayName("같은 값끼리는 동등하다")
	void sameSizesAreEqual() {
		assertThat(new VideoFileSize(1024L)).isEqualTo(new VideoFileSize(1024L));
	}
}
