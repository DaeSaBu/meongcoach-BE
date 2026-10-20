package com.daesabu.meongcoach.media.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedImageTypeException;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedVideoTypeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CommunityMediaUploadUrlIssuerTest {

	private static final String R2_UPLOAD_BASE_URL = "https://test-account.r2.cloudflarestorage.com/test-bucket/";
	private static final String R2_PUBLIC_BASE_URL = "https://images.test.meongcoach.com/";
	private static final long VALID_SIZE = 10L * 1024 * 1024;
	private static final long MAX_SIZE = 536_870_912L;

	@Autowired
	private CommunityMediaUploadUrlIssuer issuer;

	@Test
	void 이미지는_사용자의_커뮤니티_경로로_R2_업로드_URL을_발급한다() {
		CommunityMediaUploadUrlResult result = issuer.issueImage(7L, "image/png");

		assertThat(result.uploadUrl()).startsWith(R2_UPLOAD_BASE_URL + "images/community-post/7/");
		assertThat(result.publicUrl())
				.startsWith(R2_PUBLIC_BASE_URL + "images/community-post/7/")
				.endsWith(".png");
		assertThat(result.expiresInSeconds()).isEqualTo(600L);
	}

	@Test
	void 이미지_업로드_URL은_파일_크기를_서명하지_않는다() {
		CommunityMediaUploadUrlResult result = issuer.issueImage(7L, "image/jpeg");

		assertThat(result.uploadUrl()).contains("X-Amz-SignedHeaders=content-type%3Bhost");
	}

	@Test
	void 지원하지_않는_이미지_형식이면_발급에_실패한다() {
		assertThatThrownBy(() -> issuer.issueImage(7L, "image/gif"))
				.isInstanceOf(UnsupportedImageTypeException.class);
	}

	@Test
	void 영상은_사용자의_커뮤니티_경로로_R2_업로드_URL을_발급한다() {
		CommunityMediaUploadUrlResult result = issuer.issueVideo(7L, "video/quicktime", VALID_SIZE);

		assertThat(result.uploadUrl()).startsWith(R2_UPLOAD_BASE_URL + "videos/community-post/7/");
		assertThat(result.publicUrl())
				.startsWith(R2_PUBLIC_BASE_URL + "videos/community-post/7/")
				.endsWith(".mov");
		assertThat(result.expiresInSeconds()).isEqualTo(600L);
	}

	@Test
	void 영상_업로드_URL은_요청한_파일_크기를_서명에_포함한다() {
		CommunityMediaUploadUrlResult result = issuer.issueVideo(7L, "video/mp4", MAX_SIZE);

		assertThat(result.uploadUrl()).contains("X-Amz-SignedHeaders=content-length%3Bcontent-type%3Bhost");
	}

	@Test
	void 지원하지_않는_영상_형식이면_발급에_실패한다() {
		assertThatThrownBy(() -> issuer.issueVideo(7L, "video/webm", VALID_SIZE))
				.isInstanceOf(UnsupportedVideoTypeException.class);
	}

	@Test
	void 영상이_512MiB를_넘으면_발급에_실패한다() {
		assertThatThrownBy(() -> issuer.issueVideo(7L, "video/mp4", MAX_SIZE + 1))
				.isInstanceOf(VideoFileSizeExceededException.class);
	}

	@Test
	void 영상_크기가_0_이하면_발급에_실패한다() {
		assertThatThrownBy(() -> issuer.issueVideo(7L, "video/mp4", 0L))
				.isInstanceOf(InvalidVideoFileSizeException.class);
	}
}
