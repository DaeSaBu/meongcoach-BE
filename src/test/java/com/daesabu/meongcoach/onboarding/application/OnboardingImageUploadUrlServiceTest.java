package com.daesabu.meongcoach.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("온보딩 이미지 업로드 URL 발급 서비스")
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
	@DisplayName("요청한 사용자·대상·형식 그대로 media에 발급을 위임한다")
	void issueDelegatesToMedia() {
		service.issue(7L, "USER_PROFILE", "image/jpeg");

		assertThat(imageUploadUrlIssuer.issuedRequests).containsExactly("7:USER_PROFILE:image/jpeg");
	}

	@Test
	@DisplayName("media 발급 결과를 온보딩 결과로 재매핑해 반환한다")
	void issueRemapsMediaResult() {
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
