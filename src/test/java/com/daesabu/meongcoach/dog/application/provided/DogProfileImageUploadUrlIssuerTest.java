package com.daesabu.meongcoach.dog.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedImageTypeException;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class DogProfileImageUploadUrlIssuerTest {

	private static final String R2_UPLOAD_BASE_URL = "https://test-account.r2.cloudflarestorage.com/test-bucket/";
	private static final String R2_PUBLIC_BASE_URL = "https://images.test.meongcoach.com/";

	@Autowired
	private DogProfileImageUploadUrlIssuer dogProfileImageUploadUrlIssuer;

	@Test
	void 강아지_프로필_경로의_업로드_URL을_발급한다() {
		MediaUploadUrlResult result = dogProfileImageUploadUrlIssuer.issue(7L, "image/png");

		assertThat(result.uploadUrl()).startsWith(R2_UPLOAD_BASE_URL + "images/dog-profile/7/");
		assertThat(result.publicUrl())
				.startsWith(R2_PUBLIC_BASE_URL + "images/dog-profile/7/")
				.endsWith(".png");
		assertThat(result.expiresInSeconds()).isEqualTo(600L);
	}

	@Test
	void 지원하지_않는_이미지_형식이면_발급에_실패한다() {
		assertThatThrownBy(() -> dogProfileImageUploadUrlIssuer.issue(7L, "image/gif"))
				.isInstanceOf(UnsupportedImageTypeException.class);
	}
}
