package com.daesabu.meongcoach.media.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.application.required.VideoDownloadUrl;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * presign은 로컬 서명 연산이라 네트워크 없이 실제 S3Presigner로 검증한다.
 */
class S3VideoStorageTest {

	private static final String REGION = "ap-northeast-2";
	private static final String BUCKET = "test-video-bucket";
	private static final String PUBLIC_BASE_URL = "https://videos.test.meongcoach.com";
	private static final VideoObjectKey KEY =
			new VideoObjectKey("videos/training/1/550e8400-e29b-41d4-a716-446655440000.mp4");
	private static final long CONTENT_LENGTH = 10L * 1024 * 1024;

	private S3VideoStorage storage;

	@BeforeEach
	void setUp() {
		storage = new S3VideoStorage(new S3Properties(REGION, "test-access-key", "test-secret-key",
				BUCKET, PUBLIC_BASE_URL, Duration.ofMinutes(15), Duration.ofHours(1)));
	}

	@Test
	void 업로드_URL은_가상_호스팅_스타일의_버킷_키_경로를_가리킨다() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl())
				.startsWith("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/" + KEY.value());
	}

	@Test
	void 업로드_URL은_서명과_유효_시간을_담은_presigned_URL이다() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("X-Amz-Signature=");
		assertThat(url.uploadUrl()).contains("X-Amz-Expires=900");
	}

	@Test
	void 서명_대상_헤더에_contentLength가_포함된다() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		// Content-Length가 서명에 들어가야 신고한 크기와 다른 업로드를 S3가 403으로 거부한다
		assertThat(url.uploadUrl()).contains("X-Amz-SignedHeaders=content-length%3Bcontent-type%3Bhost");
	}

	@Test
	void 서명_자격_증명은_설정한_리전을_사용한다() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("%2F" + REGION + "%2Fs3%2Faws4_request");
	}

	@Test
	void 공개_URL은_공개_도메인_아래의_키_경로다() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + KEY.value());
	}

	@Test
	void 객체_키를_그대로_함께_반환한다() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.objectKey()).isEqualTo(KEY.value());
	}

	@Test
	void 유효_시간을_초_단위로_알려준다() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.expiresInSeconds()).isEqualTo(900L);
	}

	@Test
	void 다운로드_URL은_가상_호스팅_스타일의_버킷_키_경로를_가리킨다() {
		VideoDownloadUrl url = storage.issueDownloadUrl(KEY);

		assertThat(url.downloadUrl())
				.startsWith("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/" + KEY.value());
	}

	@Test
	void 다운로드_URL은_서명과_다운로드_전용_유효_시간을_담은_presigned_URL이다() {
		VideoDownloadUrl url = storage.issueDownloadUrl(KEY);

		assertThat(url.downloadUrl()).contains("X-Amz-Signature=");
		assertThat(url.downloadUrl()).contains("X-Amz-Expires=3600");
	}

	@Test
	void 다운로드_결과의_공개_URL은_공개_도메인_아래의_키_경로다() {
		VideoDownloadUrl url = storage.issueDownloadUrl(KEY);

		assertThat(url.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + KEY.value());
	}

	@Test
	void 다운로드_유효_시간을_초_단위로_알려준다() {
		VideoDownloadUrl url = storage.issueDownloadUrl(KEY);

		assertThat(url.expiresInSeconds()).isEqualTo(3600L);
	}
}
