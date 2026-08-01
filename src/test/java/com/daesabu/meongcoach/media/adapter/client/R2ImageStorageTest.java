package com.daesabu.meongcoach.media.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.application.required.ImageUploadUrl;
import com.daesabu.meongcoach.media.domain.vo.ImageObjectKey;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * presign은 로컬 서명 연산이라 네트워크 없이 실제 S3Presigner로 검증한다.
 */
@DisplayName("R2 이미지 스토리지")
class R2ImageStorageTest {

	private static final String ENDPOINT = "https://test-account.r2.cloudflarestorage.com";
	private static final String BUCKET = "test-bucket";
	private static final String PUBLIC_BASE_URL = "https://images.test.meongcoach.com";
	private static final ImageObjectKey KEY =
			new ImageObjectKey("images/user-profile/1/550e8400-e29b-41d4-a716-446655440000.jpg");

	private R2ImageStorage storage;

	@BeforeEach
	void setUp() {
		storage = new R2ImageStorage(new R2Properties(ENDPOINT, "test-access-key", "test-secret-key",
				BUCKET, PUBLIC_BASE_URL, Duration.ofMinutes(10), Duration.ofMinutes(30)));
	}

	@Test
	@DisplayName("업로드 URL은 R2 엔드포인트의 버킷·키 경로를 가리킨다")
	void uploadUrlPointsToBucketAndKey() {
		ImageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg");

		assertThat(url.uploadUrl()).startsWith(ENDPOINT + "/" + BUCKET + "/" + KEY.value());
	}

	@Test
	@DisplayName("업로드 URL은 서명과 유효 시간을 담은 presigned URL이다")
	void uploadUrlIsPresigned() {
		ImageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg");

		assertThat(url.uploadUrl()).contains("X-Amz-Signature=");
		assertThat(url.uploadUrl()).contains("X-Amz-Expires=600");
	}

	@Test
	@DisplayName("서명 대상 헤더는 Content-Type과 host뿐이다")
	void uploadUrlSignsOnlyContentTypeAndHost() {
		ImageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg");

		// 체크섬 등 다른 헤더가 서명에 끼면 클라이언트가 그 헤더 없이 PUT할 때 R2가 거부한다
		assertThat(url.uploadUrl()).contains("X-Amz-SignedHeaders=content-type%3Bhost");
	}

	@Test
	@DisplayName("공개 URL은 공개 도메인 아래의 키 경로다")
	void publicUrlIsUnderPublicBaseUrl() {
		ImageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg");

		assertThat(url.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + KEY.value());
	}

	@Test
	@DisplayName("유효 시간을 초 단위로 알려준다")
	void expiresInSecondsMatchesValidity() {
		ImageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg");

		assertThat(url.expiresInSeconds()).isEqualTo(600L);
	}

	@Test
	@DisplayName("공개 도메인 아래의 URL만 우리 공개 URL로 판별한다")
	void isPublicUrlChecksPrefix() {
		assertThat(storage.isPublicUrl(PUBLIC_BASE_URL + "/" + KEY.value())).isTrue();
		assertThat(storage.isPublicUrl("https://evil.example.com/" + KEY.value())).isFalse();
		assertThat(storage.isPublicUrl(null)).isFalse();
	}

	@Test
	@DisplayName("공개 도메인이 앞부분만 일치하는 다른 도메인은 거부한다")
	void isPublicUrlRejectsLookAlikeDomain() {
		assertThat(storage.isPublicUrl(PUBLIC_BASE_URL + ".evil.example.com/a.jpg")).isFalse();
	}
}
