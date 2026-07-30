package com.daesabu.meongcoach.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.UnsupportedImageTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이미지 형식")
class ImageTypeTest {

	@Test
	@DisplayName("Content-Type을 enum으로 변환한다")
	void fromContentTypeConvertsToEnum() {
		assertThat(ImageType.fromContentType("image/jpeg")).isEqualTo(ImageType.JPEG);
		assertThat(ImageType.fromContentType("image/png")).isEqualTo(ImageType.PNG);
		assertThat(ImageType.fromContentType("image/webp")).isEqualTo(ImageType.WEBP);
	}

	@Test
	@DisplayName("형식마다 객체 키 확장자를 가진다")
	void typeHasExtension() {
		assertThat(ImageType.JPEG.getExtension()).isEqualTo("jpg");
		assertThat(ImageType.PNG.getExtension()).isEqualTo("png");
		assertThat(ImageType.WEBP.getExtension()).isEqualTo("webp");
	}

	@Test
	@DisplayName("지원하지 않는 Content-Type이면 변환에 실패한다")
	void fromContentTypeFailsWhenUnsupported() {
		assertThatThrownBy(() -> ImageType.fromContentType("image/gif"))
				.isInstanceOf(UnsupportedImageTypeException.class);
	}

	@Test
	@DisplayName("Content-Type이 없으면 변환에 실패한다")
	void fromContentTypeFailsWhenNull() {
		assertThatThrownBy(() -> ImageType.fromContentType(null))
				.isInstanceOf(UnsupportedImageTypeException.class);
	}
}
