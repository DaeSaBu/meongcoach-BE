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
		assertThat(VideoType.fromContentType("video/quicktime")).isEqualTo(VideoType.QUICKTIME);
	}

	@Test
	@DisplayName("형식마다 객체 키 확장자를 가진다")
	void typeHasExtension() {
		assertThat(VideoType.MP4.getExtension()).isEqualTo("mp4");
		assertThat(VideoType.QUICKTIME.getExtension()).isEqualTo("mov");
	}

	@Test
	@DisplayName("형식마다 Content-Type을 가진다")
	void typeHasContentType() {
		assertThat(VideoType.MP4.getContentType()).isEqualTo("video/mp4");
		assertThat(VideoType.QUICKTIME.getContentType()).isEqualTo("video/quicktime");
	}

	@Test
	@DisplayName("지원하지 않는 Content-Type이면 변환에 실패한다")
	void fromContentTypeFailsWhenUnsupported() {
		assertThatThrownBy(() -> VideoType.fromContentType("video/avi"))
				.isInstanceOf(UnsupportedVideoTypeException.class);
	}

	@Test
	@DisplayName("Content-Type이 없으면 변환에 실패한다")
	void fromContentTypeFailsWhenNull() {
		assertThatThrownBy(() -> VideoType.fromContentType(null))
				.isInstanceOf(UnsupportedVideoTypeException.class);
	}

	@Test
	@DisplayName("확장자를 enum으로 변환한다")
	void fromExtensionConvertsToEnum() {
		assertThat(VideoType.fromExtension("mp4")).isEqualTo(VideoType.MP4);
		assertThat(VideoType.fromExtension("mov")).isEqualTo(VideoType.QUICKTIME);
	}

	@Test
	@DisplayName("허용 목록에 없는 확장자면 변환에 실패한다")
	void fromExtensionFailsWhenUnsupported() {
		assertThatThrownBy(() -> VideoType.fromExtension("avi"))
				.isInstanceOf(UnsupportedVideoTypeException.class);
	}

	@Test
	@DisplayName("확장자가 없으면 변환에 실패한다")
	void fromExtensionFailsWhenNull() {
		assertThatThrownBy(() -> VideoType.fromExtension(null))
				.isInstanceOf(UnsupportedVideoTypeException.class);
	}
}
