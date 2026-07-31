package com.daesabu.meongcoach.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.ImageStorage;
import com.daesabu.meongcoach.media.application.required.ImageUploadUrl;
import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.ImageUploadTarget;
import com.daesabu.meongcoach.media.domain.exception.InvalidUploadTargetException;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedImageTypeException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이미지 업로드 URL 발급 서비스")
class ImageUploadUrlIssueServiceTest {

	private RecordingImageStorage imageStorage;
	private ImageUploadUrlIssueService service;

	@BeforeEach
	void setUp() {
		imageStorage = new RecordingImageStorage();
		service = new ImageUploadUrlIssueService(imageStorage);
	}

	@Test
	@DisplayName("업로드 대상·사용자·이미지 형식으로 만든 객체 키를 스토리지에 넘긴다")
	void issuePassesGeneratedKeyToStorage() {
		service.issue(7L, "DOG_PROFILE", "image/png");

		// 키 포맷 자체는 ImageObjectKeyTest가 검증한다. 여기서는 변환한 값이 그대로 전달되는지만 본다
		assertThat(imageStorage.lastKey())
				.startsWith("images/" + ImageUploadTarget.DOG_PROFILE.getPathSegment() + "/7/")
				.endsWith("." + ImageType.PNG.getExtension());
	}

	@Test
	@DisplayName("요청한 Content-Type 그대로 스토리지에 전달한다")
	void issuePassesContentTypeToStorage() {
		service.issue(7L, "USER_PROFILE", "image/webp");

		assertThat(imageStorage.lastContentType()).isEqualTo("image/webp");
	}

	@Test
	@DisplayName("스토리지가 발급한 URL을 결과로 반환한다")
	void issueReturnsStorageUrls() {
		ImageUploadUrlResult result = service.issue(7L, "USER_PROFILE", "image/jpeg");

		assertThat(result.uploadUrl()).isEqualTo("https://storage.test/upload");
		assertThat(result.publicUrl()).isEqualTo("https://images.test/public");
		assertThat(result.expiresInSeconds()).isEqualTo(600L);
	}

	@Test
	@DisplayName("지원하지 않는 업로드 대상이면 발급에 실패한다")
	void issueFailsWhenTargetIsInvalid() {
		assertThatThrownBy(() -> service.issue(7L, "BANNER", "image/jpeg"))
				.isInstanceOf(InvalidUploadTargetException.class);
		assertThat(imageStorage.keys).isEmpty();
	}

	@Test
	@DisplayName("지원하지 않는 이미지 형식이면 발급에 실패한다")
	void issueFailsWhenContentTypeIsUnsupported() {
		assertThatThrownBy(() -> service.issue(7L, "USER_PROFILE", "image/gif"))
				.isInstanceOf(UnsupportedImageTypeException.class);
		assertThat(imageStorage.keys).isEmpty();
	}

	private static class RecordingImageStorage implements ImageStorage {

		private final List<String> keys = new ArrayList<>();
		private final List<String> contentTypes = new ArrayList<>();

		@Override
		public ImageUploadUrl issueUploadUrl(String key, String contentType) {
			keys.add(key);
			contentTypes.add(contentType);
			return new ImageUploadUrl("https://storage.test/upload", "https://images.test/public", 600L);
		}

		@Override
		public boolean isPublicUrl(String url) {
			return true;
		}

		private String lastKey() {
			return keys.getLast();
		}

		private String lastContentType() {
			return contentTypes.getLast();
		}
	}
}
