package com.daesabu.meongcoach.onboarding.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.UnsupportedImageTypeException;
import com.daesabu.meongcoach.onboarding.application.provided.dto.OnboardingImageUploadUrlRequest;
import com.daesabu.meongcoach.onboarding.domain.exception.InvalidOnboardingImageTargetException;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class OnboardingImageUploadUrlIssuerTest {

	private static final String R2_UPLOAD_BASE_URL = "https://test-account.r2.cloudflarestorage.com/test-bucket/";
	private static final String R2_PUBLIC_BASE_URL = "https://images.test.meongcoach.com/";

	@Autowired
	private OnboardingImageUploadUrlIssuer onboardingImageUploadUrlIssuer;

	@Test
	void JPEG_강아지_프로필_이미지_업로드_URL을_발급한다() {
		OnboardingImageUploadUrlResult result = onboardingImageUploadUrlIssuer.issue(7L, new OnboardingImageUploadUrlRequest(null, "image/jpeg"));

		assertThat(result.uploadUrl()).startsWith(R2_UPLOAD_BASE_URL + "images/dog-profile/7/");
		assertThat(result.publicUrl())
				.startsWith(R2_PUBLIC_BASE_URL + "images/dog-profile/7/")
				.endsWith(".jpg");
		assertThat(result.expiresInSeconds()).isEqualTo(600L);
	}

	@Test
	void 강아지_프로필_이미지는_강아지_프로필_경로의_업로드_URL을_발급한다() {
		OnboardingImageUploadUrlResult result = onboardingImageUploadUrlIssuer.issue(7L, new OnboardingImageUploadUrlRequest(null, "image/png"));

		assertThat(result.publicUrl())
				.startsWith(R2_PUBLIC_BASE_URL + "images/dog-profile/7/")
				.endsWith(".png");
	}

	@Test
	void 구버전의_보호자_사진은_보호자_프로필_경로에_발급한다() {
		var request = new OnboardingImageUploadUrlRequest("USER_PROFILE", "image/jpeg");
		OnboardingImageUploadUrlResult result = onboardingImageUploadUrlIssuer.issue(7L, request);

		assertThat(result.publicUrl()).startsWith(R2_PUBLIC_BASE_URL + "images/user-profile/7/");
	}

	@Test
	void 구버전의_강아지_사진은_강아지_프로필_경로에_발급한다() {
		var request = new OnboardingImageUploadUrlRequest("DOG_PROFILE", "image/jpeg");
		OnboardingImageUploadUrlResult result = onboardingImageUploadUrlIssuer.issue(7L, request);

		assertThat(result.publicUrl()).startsWith(R2_PUBLIC_BASE_URL + "images/dog-profile/7/");
	}

	@Test
	void 알_수_없는_대상은_임의의_경로로_발급하지_않는다() {
		var request = new OnboardingImageUploadUrlRequest("COMMUNITY_POST", "image/jpeg");
		assertThatThrownBy(() -> onboardingImageUploadUrlIssuer.issue(7L, request))
				.isInstanceOf(InvalidOnboardingImageTargetException.class);
	}

	@Test
	void 지원하지_않는_이미지_형식이면_발급에_실패한다() {
		assertThatThrownBy(() -> onboardingImageUploadUrlIssuer.issue(7L, new OnboardingImageUploadUrlRequest(null, "image/gif")))
				.isInstanceOf(UnsupportedImageTypeException.class);
	}
}
