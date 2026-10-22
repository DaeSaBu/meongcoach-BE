package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.provided.dto.PostMediaUploadUrlRequest;
import com.daesabu.meongcoach.community.domain.exception.InvalidPostMediaTypeException;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedImageTypeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class PostMediaUploadUrlIssuerTest {

	private static final Long USER_ID = 42L;
	private static final String R2_PUBLIC_BASE_URL = "https://images.test.meongcoach.com/";
	private static final long VALID_SIZE = 10L * 1024 * 1024;

	@Autowired
	private PostMediaUploadUrlIssuer postMediaUploadUrlIssuer;

	@Test
	void 사진이면_커뮤니티_이미지_업로드_URL을_발급한다() {
		MediaUploadUrlResult result = postMediaUploadUrlIssuer.issue(USER_ID,
				new PostMediaUploadUrlRequest("IMAGE", "image/jpeg", VALID_SIZE));

		assertThat(result.publicUrl())
				.startsWith(R2_PUBLIC_BASE_URL + "images/community-post/42/")
				.endsWith(".jpg");
	}

	@Test
	void 영상이면_커뮤니티_영상_업로드_URL을_발급한다() {
		MediaUploadUrlResult result = postMediaUploadUrlIssuer.issue(USER_ID,
				new PostMediaUploadUrlRequest("VIDEO", "video/mp4", VALID_SIZE));

		assertThat(result.publicUrl())
				.startsWith(R2_PUBLIC_BASE_URL + "videos/community-post/42/")
				.endsWith(".mp4");
	}

	@Test
	void 미디어_종류는_대소문자를_구분하지_않는다() {
		MediaUploadUrlResult result = postMediaUploadUrlIssuer.issue(USER_ID,
				new PostMediaUploadUrlRequest("video", "video/mp4", VALID_SIZE));

		assertThat(result.publicUrl()).contains("videos/community-post/42/");
	}

	@Test
	void 사진은_크기_상한_없이_발급한다() {
		MediaUploadUrlResult result = postMediaUploadUrlIssuer.issue(USER_ID,
				new PostMediaUploadUrlRequest("IMAGE", "image/png", 1024L * 1024 * 1024));

		assertThat(result.publicUrl()).endsWith(".png");
	}

	@Test
	void 영상이_512MiB를_넘으면_발급에_실패한다() {
		PostMediaUploadUrlRequest request = new PostMediaUploadUrlRequest("VIDEO", "video/mp4", 536_870_913L);

		assertThatThrownBy(() -> postMediaUploadUrlIssuer.issue(USER_ID, request))
				.isInstanceOf(VideoFileSizeExceededException.class);
	}

	@Test
	void 미디어_종류와_형식이_맞지_않으면_발급에_실패한다() {
		PostMediaUploadUrlRequest request = new PostMediaUploadUrlRequest("IMAGE", "video/mp4", VALID_SIZE);

		assertThatThrownBy(() -> postMediaUploadUrlIssuer.issue(USER_ID, request))
				.isInstanceOf(UnsupportedImageTypeException.class);
	}

	@Test
	void 지원하지_않는_미디어_종류면_발급에_실패한다() {
		PostMediaUploadUrlRequest request = new PostMediaUploadUrlRequest("AUDIO", "audio/mpeg", VALID_SIZE);

		assertThatThrownBy(() -> postMediaUploadUrlIssuer.issue(USER_ID, request))
				.isInstanceOf(InvalidPostMediaTypeException.class);
	}

	@Test
	void 파일_크기가_없으면_발급에_실패한다() {
		PostMediaUploadUrlRequest request = new PostMediaUploadUrlRequest("VIDEO", "video/mp4", null);

		assertThatThrownBy(() -> postMediaUploadUrlIssuer.issue(USER_ID, request))
				.isInstanceOf(ConstraintViolationException.class);
	}
}
