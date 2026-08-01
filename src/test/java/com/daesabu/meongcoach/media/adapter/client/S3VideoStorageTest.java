package com.daesabu.meongcoach.media.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * presign은 로컬 서명 연산이라 네트워크 없이 실제 S3Presigner로 검증한다.
 */
@DisplayName("S3 영상 스토리지")
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
				BUCKET, PUBLIC_BASE_URL, Duration.ofMinutes(15)));
	}

	@Test
	@DisplayName("업로드 URL은 가상 호스팅 스타일의 버킷·키 경로를 가리킨다")
	void uploadUrlPointsToVirtualHostedBucketAndKey() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl())
				.startsWith("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/" + KEY.value());
	}

	@Test
	@DisplayName("업로드 URL은 서명과 유효 시간을 담은 presigned URL이다")
	void uploadUrlIsPresigned() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("X-Amz-Signature=");
		assertThat(url.uploadUrl()).contains("X-Amz-Expires=900");
	}

	@Test
	@DisplayName("서명 대상 헤더에 Content-Length가 포함된다")
	void uploadUrlSignsContentLengthAndContentTypeAndHost() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		// Content-Length가 서명에 들어가야 신고한 크기와 다른 업로드를 S3가 403으로 거부한다
		assertThat(url.uploadUrl()).contains("X-Amz-SignedHeaders=content-length%3Bcontent-type%3Bhost");
	}

	@Test
	@DisplayName("서명 자격 증명은 설정한 리전을 사용한다")
	void uploadUrlCredentialUsesConfiguredRegion() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("%2F" + REGION + "%2Fs3%2Faws4_request");
	}

	@Test
	@DisplayName("공개 URL은 공개 도메인 아래의 키 경로다")
	void publicUrlIsUnderPublicBaseUrl() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + KEY.value());
	}

	@Test
	@DisplayName("객체 키를 그대로 함께 반환한다")
	void objectKeyIsReturnedAsIs() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.objectKey()).isEqualTo(KEY.value());
	}

	@Test
	@DisplayName("유효 시간을 초 단위로 알려준다")
	void expiresInSecondsMatchesValidity() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.expiresInSeconds()).isEqualTo(900L);
	}
}
