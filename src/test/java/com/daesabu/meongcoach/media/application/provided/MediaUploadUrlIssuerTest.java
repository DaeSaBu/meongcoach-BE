package com.daesabu.meongcoach.media.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedImageTypeException;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedVideoTypeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class MediaUploadUrlIssuerTest {

	private static final String R2_UPLOAD_BASE_URL = "https://test-account.r2.cloudflarestorage.com/test-bucket/";
	private static final String R2_PUBLIC_BASE_URL = "https://images.test.meongcoach.com/";
	private static final String S3_UPLOAD_BASE_URL = "https://test-video-bucket.s3.ap-northeast-2.amazonaws.com/";
	private static final String S3_PUBLIC_BASE_URL = "https://videos.test.meongcoach.com/";
	private static final MediaScope PUBLIC_IMAGE = MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC, "test-image");
	private static final MediaScope PUBLIC_VIDEO = MediaScope.of(MediaKind.VIDEO, MediaVisibility.PUBLIC, "test-video");
	private static final MediaScope PRIVATE_VIDEO =
			MediaScope.of(MediaKind.VIDEO, MediaVisibility.PRIVATE, "test-video");
	private static final String GCS_BASE_URL = "https://storage.googleapis.com/test-ai-video-bucket/";
	private static final MediaScope ANALYSIS_VIDEO =
			MediaScope.of(MediaKind.VIDEO, MediaVisibility.ANALYSIS, "test-video");
	private static final long MAX_BYTES = 1_024L;

	@Autowired
	private MediaUploadUrlIssuer issuer;

	@Test
	void 공개_범위_이미지는_사용자_경로의_R2_업로드_URL과_공개_URL을_발급한다() {
		MediaUploadUrlResult result = issuer.issue(MediaUploadRequest.image(PUBLIC_IMAGE, 7L, "image/png"));

		assertThat(result.objectKey()).matches("images/test-image/7/[0-9a-f-]{36}\\.png");
		assertThat(result.uploadUrl()).startsWith(R2_UPLOAD_BASE_URL + result.objectKey());
		assertThat(result.publicUrl()).isEqualTo(R2_PUBLIC_BASE_URL + result.objectKey());
		assertThat(result.expiresInSeconds()).isEqualTo(600L);
	}

	@Test
	void 이미지_업로드_URL은_파일_크기를_서명하지_않는다() {
		MediaUploadUrlResult result = issuer.issue(MediaUploadRequest.image(PUBLIC_IMAGE, 7L, "image/jpeg"));

		assertThat(result.uploadUrl()).contains("X-Amz-SignedHeaders=content-type%3Bhost");
	}

	@Test
	void 공개_범위_영상은_요청한_파일_크기를_R2_업로드_URL_서명에_포함한다() {
		MediaUploadUrlResult result =
				issuer.issue(MediaUploadRequest.video(PUBLIC_VIDEO, 7L, "video/quicktime", MAX_BYTES, MAX_BYTES));

		assertThat(result.objectKey()).matches("videos/test-video/7/[0-9a-f-]{36}\\.mov");
		assertThat(result.uploadUrl())
				.startsWith(R2_UPLOAD_BASE_URL + result.objectKey())
				.contains("X-Amz-SignedHeaders=content-length%3Bcontent-type%3Bhost");
		assertThat(result.publicUrl()).isEqualTo(R2_PUBLIC_BASE_URL + result.objectKey());
	}

	@Test
	void 비공개_범위_영상은_S3_업로드_URL을_발급한다() {
		MediaUploadUrlResult result =
				issuer.issue(MediaUploadRequest.video(PRIVATE_VIDEO, 7L, "video/mp4", MAX_BYTES, MAX_BYTES));

		assertThat(result.objectKey()).matches("videos/test-video/7/[0-9a-f-]{36}\\.mp4");
		assertThat(result.uploadUrl())
				.startsWith(S3_UPLOAD_BASE_URL + result.objectKey())
				.contains("X-Amz-SignedHeaders=content-length%3Bcontent-type%3Bhost");
		assertThat(result.publicUrl()).isEqualTo(S3_PUBLIC_BASE_URL + result.objectKey());
		assertThat(result.expiresInSeconds()).isEqualTo(900L);
	}

	@Test
	void 분석_범위_영상은_파일_크기를_서명한_GCS_업로드_URL과_버킷_표준_URL을_발급한다() {
		MediaUploadUrlResult result =
				issuer.issue(MediaUploadRequest.video(ANALYSIS_VIDEO, 7L, "video/mp4", MAX_BYTES, MAX_BYTES));

		assertThat(result.objectKey()).matches("videos/test-video/7/[0-9a-f-]{36}\\.mp4");
		assertThat(result.uploadUrl())
				.startsWith(GCS_BASE_URL + result.objectKey())
				.contains("X-Goog-SignedHeaders=content-length%3Bcontent-type%3Bhost");
		assertThat(result.publicUrl()).isEqualTo(GCS_BASE_URL + result.objectKey());
		assertThat(result.expiresInSeconds()).isEqualTo(900L);
	}

	@Test
	void 하위_경로가_있는_범위면_사용자_ID_뒤에_하위_경로를_둔다() {
		MediaUploadUrlResult result = issuer.issue(
				MediaUploadRequest.video(PRIVATE_VIDEO.withSubPath("1101"), 7L, "video/mp4", MAX_BYTES, MAX_BYTES));

		assertThat(result.objectKey()).matches("videos/test-video/7/1101/[0-9a-f-]{36}\\.mp4");
	}

	@Test
	void 지원하지_않는_이미지_형식이면_발급에_실패한다() {
		assertThatThrownBy(() -> issuer.issue(MediaUploadRequest.image(PUBLIC_IMAGE, 7L, "image/gif")))
				.isInstanceOf(UnsupportedImageTypeException.class);
	}

	@Test
	void 영상_범위에_이미지_형식을_보내면_발급에_실패한다() {
		assertThatThrownBy(() -> issuer.issue(
				MediaUploadRequest.video(PUBLIC_VIDEO, 7L, "image/png", MAX_BYTES, MAX_BYTES)))
				.isInstanceOf(UnsupportedVideoTypeException.class);
	}

	@Test
	void 영상이_요청한_상한을_넘으면_발급에_실패한다() {
		assertThatThrownBy(() -> issuer.issue(
				MediaUploadRequest.video(PUBLIC_VIDEO, 7L, "video/mp4", MAX_BYTES + 1, MAX_BYTES)))
				.isInstanceOf(VideoFileSizeExceededException.class);
	}

	@Test
	void 영상_크기가_0_이하면_발급에_실패한다() {
		assertThatThrownBy(() -> issuer.issue(MediaUploadRequest.video(PUBLIC_VIDEO, 7L, "video/mp4", 0L, MAX_BYTES)))
				.isInstanceOf(InvalidVideoFileSizeException.class);
	}
}
