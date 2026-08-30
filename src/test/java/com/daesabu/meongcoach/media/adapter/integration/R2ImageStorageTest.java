package com.daesabu.meongcoach.media.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.application.required.ImageUploadUrl;
import com.daesabu.meongcoach.media.domain.vo.ImageObjectKey;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * presign은 로컬 서명 연산이라 네트워크 없이 실제 S3Presigner로 검증한다.
 */
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
				BUCKET, PUBLIC_BASE_URL, Duration.ofMinutes(10)));
	}

	@Test
	void 업로드_URL은_R2_엔드포인트의_버킷_키_경로를_가리킨다() {
		ImageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg");

		assertThat(url.uploadUrl()).startsWith(ENDPOINT + "/" + BUCKET + "/" + KEY.value());
	}

	@Test
	void 업로드_URL은_서명과_유효_시간을_담은_presigned_URL이다() {
		ImageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg");

		assertThat(url.uploadUrl()).contains("X-Amz-Signature=");
		assertThat(url.uploadUrl()).contains("X-Amz-Expires=600");
	}

	@Test
	void 서명_대상_헤더는_contentType과_host뿐이다() {
		ImageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg");

		// 체크섬 등 다른 헤더가 서명에 끼면 클라이언트가 그 헤더 없이 PUT할 때 R2가 거부한다
		assertThat(url.uploadUrl()).contains("X-Amz-SignedHeaders=content-type%3Bhost");
	}

	@Test
	void 공개_URL은_공개_도메인_아래의_키_경로다() {
		ImageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg");

		assertThat(url.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + KEY.value());
	}

	@Test
	void 유효_시간을_초_단위로_알려준다() {
		ImageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg");

		assertThat(url.expiresInSeconds()).isEqualTo(600L);
	}

	@Test
	void 공개_도메인_아래의_URL만_우리_공개_URL로_판별한다() {
		assertThat(storage.isPublicUrl(PUBLIC_BASE_URL + "/" + KEY.value())).isTrue();
		assertThat(storage.isPublicUrl("https://evil.example.com/" + KEY.value())).isFalse();
		assertThat(storage.isPublicUrl(null)).isFalse();
	}

	@Test
	void 공개_도메인이_앞부분만_일치하는_다른_도메인은_거부한다() {
		assertThat(storage.isPublicUrl(PUBLIC_BASE_URL + ".evil.example.com/a.jpg")).isFalse();
	}
}
