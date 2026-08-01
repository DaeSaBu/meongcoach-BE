package com.daesabu.meongcoach.media.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.VideoSizeExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("영상 파일 크기 값 객체")
class VideoFileSizeTest {

	@Test
	@DisplayName("1바이트는 허용한다")
	void ofAcceptsOneByte() {
		assertThat(VideoFileSize.of(1L).bytes()).isEqualTo(1L);
	}

	@Test
	@DisplayName("상한과 같은 100MB는 허용한다")
	void ofAcceptsMaxBytes() {
		assertThat(VideoFileSize.of(104_857_600L).bytes()).isEqualTo(VideoFileSize.MAX_BYTES);
	}

	@Test
	@DisplayName("상한을 1바이트 넘으면 거부한다")
	void ofRejectsOverMaxBytes() {
		assertThatThrownBy(() -> VideoFileSize.of(104_857_601L))
				.isInstanceOf(VideoSizeExceededException.class);
	}

	@Test
	@DisplayName("0바이트는 거부한다")
	void ofRejectsZero() {
		assertThatThrownBy(() -> VideoFileSize.of(0L))
				.isInstanceOf(VideoSizeExceededException.class);
	}

	@Test
	@DisplayName("음수는 거부한다")
	void ofRejectsNegative() {
		assertThatThrownBy(() -> VideoFileSize.of(-1L))
				.isInstanceOf(VideoSizeExceededException.class);
	}

	@Test
	@DisplayName("같은 크기끼리는 동등하다")
	void sameSizesAreEqual() {
		assertThat(VideoFileSize.of(1_024L)).isEqualTo(VideoFileSize.of(1_024L));
	}
}
