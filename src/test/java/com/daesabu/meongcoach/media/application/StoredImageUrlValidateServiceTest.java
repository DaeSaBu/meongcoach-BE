package com.daesabu.meongcoach.media.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.application.required.ImageStorage;
import com.daesabu.meongcoach.media.application.required.ImageUploadUrl;
import com.daesabu.meongcoach.media.domain.exception.InvalidImageUrlException;
import com.daesabu.meongcoach.media.domain.vo.ImageObjectKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StoredImageUrlValidateServiceTest {

	private static final String PUBLIC_BASE_URL = "https://images.test.meongcoach.com/";

	private StoredImageUrlValidateService service;

	@BeforeEach
	void setUp() {
		service = new StoredImageUrlValidateService(new PrefixImageStorage());
	}

	@Test
	void 우리_스토리지의_공개_URL이면_통과한다() {
		assertThatCode(() -> service.validate(PUBLIC_BASE_URL + "images/user-profile/1/a.jpg"))
				.doesNotThrowAnyException();
	}

	@Test
	void 이미지가_null이면_통과한다() {
		assertThatCode(() -> service.validate(null)).doesNotThrowAnyException();
	}

	@Test
	void 이미지가_빈_문자열이면_통과한다() {
		assertThatCode(() -> service.validate(" ")).doesNotThrowAnyException();
	}

	@Test
	void 외부_URL이면_검증에_실패한다() {
		assertThatThrownBy(() -> service.validate("https://evil.example.com/a.jpg"))
				.isInstanceOf(InvalidImageUrlException.class);
	}

	private static class PrefixImageStorage implements ImageStorage {

		@Override
		public ImageUploadUrl issueUploadUrl(ImageObjectKey key, String contentType) {
			throw new UnsupportedOperationException("이 테스트에서 쓰지 않는다");
		}

		@Override
		public boolean isPublicUrl(String url) {
			return url != null && url.startsWith(PUBLIC_BASE_URL);
		}
	}
}
