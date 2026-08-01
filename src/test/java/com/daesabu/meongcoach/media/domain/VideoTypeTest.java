package com.daesabu.meongcoach.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.UnsupportedVideoTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("영상 형식")
class VideoTypeTest {

	@Test
	@DisplayName("Content-Type을 enum으로 변환한다")
	void fromContentTypeConvertsToEnum() {
		assertThat(VideoType.fromContentType("video/mp4")).isEqualTo(VideoType.MP4);
		assertThat(VideoType.fromContentType("video/quicktime")).isEqualTo(VideoType.MOV);
	}

	@Test
	@DisplayName("형식마다 객체 키 확장자를 가진다")
	void typeHasExtension() {
		assertThat(VideoType.MP4.getExtension()).isEqualTo("mp4");
		assertThat(VideoType.MOV.getExtension()).isEqualTo("mov");
	}

	@Test
	@DisplayName("지원하지 않는 Content-Type이면 변환에 실패한다")
	void fromContentTypeFailsWhenUnsupported() {
		assertThatThrownBy(() -> VideoType.fromContentType("video/webm"))
				.isInstanceOf(UnsupportedVideoTypeException.class);
	}

	@Test
	@DisplayName("이미지 Content-Type이면 변환에 실패한다")
	void fromContentTypeFailsWhenImageContentType() {
		assertThatThrownBy(() -> VideoType.fromContentType("image/jpeg"))
				.isInstanceOf(UnsupportedVideoTypeException.class);
	}

	@Test
	@DisplayName("Content-Type이 없으면 변환에 실패한다")
	void fromContentTypeFailsWhenNull() {
		assertThatThrownBy(() -> VideoType.fromContentType(null))
				.isInstanceOf(UnsupportedVideoTypeException.class);
	}
}
