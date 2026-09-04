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
import com.daesabu.meongcoach.media.domain.vo.ImageObjectKey;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImageUploadUrlIssueServiceTest {

	private RecordingImageStorage imageStorage;
	private ImageUploadUrlIssueService service;

	@BeforeEach
	void setUp() {
		imageStorage = new RecordingImageStorage();
		service = new ImageUploadUrlIssueService(imageStorage);
	}

	@Test
	void 업로드_대상_사용자_이미지_형식으로_만든_객체_키를_스토리지에_넘긴다() {
		service.issue(7L, "DOG_PROFILE", "image/png");

		// 키 포맷 자체는 ImageObjectKeyTest가 검증한다. 여기서는 변환한 값이 그대로 전달되는지만 본다
		assertThat(imageStorage.lastKey().value())
				.startsWith("images/" + ImageUploadTarget.DOG_PROFILE.getPathSegment() + "/7/")
				.endsWith("." + ImageType.PNG.getExtension());
	}

	@Test
	void 요청한_contentType_그대로_스토리지에_전달한다() {
		service.issue(7L, "USER_PROFILE", "image/webp");

		assertThat(imageStorage.lastContentType()).isEqualTo("image/webp");
	}

	@Test
	void 스토리지가_발급한_URL을_결과로_반환한다() {
		ImageUploadUrlResult result = service.issue(7L, "USER_PROFILE", "image/jpeg");

		assertThat(result.uploadUrl()).isEqualTo("https://storage.test/upload");
		assertThat(result.publicUrl()).isEqualTo("https://images.test/public");
		assertThat(result.expiresInSeconds()).isEqualTo(600L);
	}

	@Test
	void 지원하지_않는_업로드_대상이면_발급에_실패한다() {
		assertThatThrownBy(() -> service.issue(7L, "BANNER", "image/jpeg"))
				.isInstanceOf(InvalidUploadTargetException.class);
		assertThat(imageStorage.keys).isEmpty();
	}

	@Test
	void 지원하지_않는_이미지_형식이면_발급에_실패한다() {
		assertThatThrownBy(() -> service.issue(7L, "USER_PROFILE", "image/gif"))
				.isInstanceOf(UnsupportedImageTypeException.class);
		assertThat(imageStorage.keys).isEmpty();
	}

	private static class RecordingImageStorage implements ImageStorage {

		private final List<ImageObjectKey> keys = new ArrayList<>();
		private final List<String> contentTypes = new ArrayList<>();

		@Override
		public ImageUploadUrl issueUploadUrl(ImageObjectKey key, String contentType) {
			keys.add(key);
			contentTypes.add(contentType);
			return new ImageUploadUrl("https://storage.test/upload", "https://images.test/public", 600L);
		}

		@Override
		public boolean isPublicUrl(String url) {
			return true;
		}

		private ImageObjectKey lastKey() {
			return keys.getLast();
		}

		private String lastContentType() {
			return contentTypes.getLast();
		}
	}
}
