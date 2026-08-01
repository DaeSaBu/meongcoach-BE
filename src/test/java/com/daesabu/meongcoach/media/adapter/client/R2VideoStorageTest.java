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
@DisplayName("R2 영상 스토리지")
class R2VideoStorageTest {

	private static final String ENDPOINT = "https://test-account.r2.cloudflarestorage.com";
	private static final String BUCKET = "test-bucket";
	private static final String PUBLIC_BASE_URL = "https://videos.test.meongcoach.com";
	private static final VideoObjectKey KEY =
			new VideoObjectKey("videos/ai-analysis/1/550e8400-e29b-41d4-a716-446655440000.mp4");
	private static final String CONTENT_TYPE = "video/mp4";
	private static final long CONTENT_LENGTH = 10_485_760L;

	private R2VideoStorage storage;

	@BeforeEach
	void setUp() {
		storage = new R2VideoStorage(new R2Properties(ENDPOINT, "test-access-key", "test-secret-key",
				BUCKET, PUBLIC_BASE_URL, Duration.ofMinutes(10), Duration.ofMinutes(30)));
	}

	@Test
	@DisplayName("업로드 URL은 R2 엔드포인트의 버킷·키 경로를 가리킨다")
	void uploadUrlPointsToBucketAndKey() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, CONTENT_TYPE, CONTENT_LENGTH);

		assertThat(url.uploadUrl()).startsWith(ENDPOINT + "/" + BUCKET + "/" + KEY.value());
	}

	@Test
	@DisplayName("업로드 URL은 서명과 영상용 유효 시간을 담은 presigned URL이다")
	void uploadUrlIsPresigned() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, CONTENT_TYPE, CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("X-Amz-Signature=");
		assertThat(url.uploadUrl()).contains("X-Amz-Expires=1800");
	}

	@Test
	@DisplayName("서명 대상 헤더에 Content-Length와 Content-Type이 모두 들어간다")
	void uploadUrlSignsContentLengthAndContentType() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, CONTENT_TYPE, CONTENT_LENGTH);

		// Content-Length가 서명에 포함돼야 클라이언트가 발급 요청과 다른 크기로 업로드하는 것을 R2가 거부한다
		assertThat(url.uploadUrl()).contains("X-Amz-SignedHeaders=content-length%3Bcontent-type%3Bhost");
	}

	@Test
	@DisplayName("공개 URL은 공개 도메인 아래의 키 경로다")
	void publicUrlIsUnderPublicBaseUrl() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, CONTENT_TYPE, CONTENT_LENGTH);

		assertThat(url.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + KEY.value());
	}

	@Test
	@DisplayName("공개 도메인의 끝 슬래시를 정리해 공개 URL을 만든다")
	void publicUrlTrimsTrailingSlashOfBaseUrl() {
		R2VideoStorage trailingSlashStorage = new R2VideoStorage(new R2Properties(ENDPOINT, "test-access-key",
				"test-secret-key", BUCKET, PUBLIC_BASE_URL + "/", Duration.ofMinutes(10), Duration.ofMinutes(30)));

		VideoUploadUrl url = trailingSlashStorage.issueUploadUrl(KEY, CONTENT_TYPE, CONTENT_LENGTH);

		assertThat(url.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + KEY.value());
	}

	@Test
	@DisplayName("유효 시간을 초 단위로 알려준다")
	void expiresInSecondsMatchesVideoValidity() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, CONTENT_TYPE, CONTENT_LENGTH);

		assertThat(url.expiresInSeconds()).isEqualTo(1800L);
	}
}
