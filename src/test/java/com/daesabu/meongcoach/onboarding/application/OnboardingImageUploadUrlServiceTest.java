package com.daesabu.meongcoach.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnboardingImageUploadUrlServiceTest {

	private static final String UPLOAD_URL = "https://storage.test/upload?X-Amz-Signature=abc";
	private static final String PUBLIC_URL = "https://images.test/images/user-profile/7/key.jpg";

	private RecordingImageUploadUrlIssuer imageUploadUrlIssuer;
	private OnboardingImageUploadUrlService service;

	@BeforeEach
	void setUp() {
		imageUploadUrlIssuer = new RecordingImageUploadUrlIssuer();
		service = new OnboardingImageUploadUrlService(imageUploadUrlIssuer);
	}

	@Test
	void 요청한_사용자_대상_형식_그대로_media에_발급을_위임한다() {
		service.issue(7L, "USER_PROFILE", "image/jpeg");

		assertThat(imageUploadUrlIssuer.issuedRequests).containsExactly("7:USER_PROFILE:image/jpeg");
	}

	@Test
	void media_발급_결과를_온보딩_결과로_재매핑해_반환한다() {
		OnboardingImageUploadUrlResult result = service.issue(7L, "DOG_PROFILE", "image/png");

		assertThat(result.uploadUrl()).isEqualTo(UPLOAD_URL);
		assertThat(result.publicUrl()).isEqualTo(PUBLIC_URL);
		assertThat(result.expiresInSeconds()).isEqualTo(600L);
	}

	private static class RecordingImageUploadUrlIssuer implements ImageUploadUrlIssuer {

		private final List<String> issuedRequests = new ArrayList<>();

		@Override
		public ImageUploadUrlResult issue(Long userId, String target, String contentType) {
			issuedRequests.add(userId + ":" + target + ":" + contentType);
			return new ImageUploadUrlResult(UPLOAD_URL, PUBLIC_URL, 600L);
		}
	}
}
