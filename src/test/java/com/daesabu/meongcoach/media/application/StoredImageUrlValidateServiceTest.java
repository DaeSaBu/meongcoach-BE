package com.daesabu.meongcoach.media.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.application.required.ImageStorage;
import com.daesabu.meongcoach.media.application.required.ImageUploadUrl;
import com.daesabu.meongcoach.media.domain.exception.InvalidImageUrlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("저장 이미지 URL 검증 서비스")
class StoredImageUrlValidateServiceTest {

	private static final String PUBLIC_BASE_URL = "https://images.test.meongcoach.com/";

	private StoredImageUrlValidateService service;

	@BeforeEach
	void setUp() {
		service = new StoredImageUrlValidateService(new PrefixImageStorage());
	}

	@Test
	@DisplayName("우리 스토리지의 공개 URL이면 통과한다")
	void validatePassesStorageUrl() {
		assertThatCode(() -> service.validate(PUBLIC_BASE_URL + "images/user-profile/1/a.jpg"))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("이미지 미설정(null)은 통과한다")
	void validatePassesNull() {
		assertThatCode(() -> service.validate(null)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("이미지 미설정(빈 문자열)은 통과한다")
	void validatePassesBlank() {
		assertThatCode(() -> service.validate(" ")).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("외부 URL이면 검증에 실패한다")
	void validateFailsWhenUrlIsExternal() {
		assertThatThrownBy(() -> service.validate("https://evil.example.com/a.jpg"))
				.isInstanceOf(InvalidImageUrlException.class);
	}

	private static class PrefixImageStorage implements ImageStorage {

		@Override
		public ImageUploadUrl issueUploadUrl(String key, String contentType) {
			throw new UnsupportedOperationException("이 테스트에서 쓰지 않는다");
		}

		@Override
		public boolean isPublicUrl(String url) {
			return url != null && url.startsWith(PUBLIC_BASE_URL);
		}
	}
}
