package com.daesabu.meongcoach.onboarding.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.UnsupportedImageTypeException;
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
	void 사용자_프로필_이미지는_사용자_프로필_경로의_업로드_URL을_발급한다() {
		OnboardingImageUploadUrlResult result = onboardingImageUploadUrlIssuer.issue(7L, "USER_PROFILE", "image/jpeg");

		assertThat(result.uploadUrl()).startsWith(R2_UPLOAD_BASE_URL + "images/user-profile/7/");
		assertThat(result.publicUrl())
				.startsWith(R2_PUBLIC_BASE_URL + "images/user-profile/7/")
				.endsWith(".jpg");
		assertThat(result.expiresInSeconds()).isEqualTo(600L);
	}

	@Test
	void 강아지_프로필_이미지는_강아지_프로필_경로의_업로드_URL을_발급한다() {
		OnboardingImageUploadUrlResult result = onboardingImageUploadUrlIssuer.issue(7L, "DOG_PROFILE", "image/png");

		assertThat(result.publicUrl())
				.startsWith(R2_PUBLIC_BASE_URL + "images/dog-profile/7/")
				.endsWith(".png");
	}

	@Test
	void 지원하지_않는_업로드_대상이면_발급에_실패한다() {
		assertThatThrownBy(() -> onboardingImageUploadUrlIssuer.issue(7L, "COMMUNITY_POST", "image/jpeg"))
				.isInstanceOf(InvalidOnboardingImageTargetException.class);
	}

	@Test
	void 지원하지_않는_이미지_형식이면_발급에_실패한다() {
		assertThatThrownBy(() -> onboardingImageUploadUrlIssuer.issue(7L, "USER_PROFILE", "image/gif"))
				.isInstanceOf(UnsupportedImageTypeException.class);
	}
}
