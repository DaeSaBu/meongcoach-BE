package com.daesabu.meongcoach.media.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.VideoObjectKey;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class R2VideoStorageTest {

	private static final String ENDPOINT = "https://test-account.r2.cloudflarestorage.com";
	private static final String BUCKET = "test-bucket";
	private static final String PUBLIC_BASE_URL = "https://images.test.meongcoach.com";
	private static final VideoObjectKey KEY =
			new VideoObjectKey("videos/community-post/1/550e8400-e29b-41d4-a716-446655440000.mp4");
	private static final long CONTENT_LENGTH = 10L * 1024 * 1024;

	private R2VideoStorage storage;

	@BeforeEach
	void setUp() {
		storage = new R2VideoStorage(new R2Properties(ENDPOINT, "test-access-key", "test-secret-key",
				BUCKET, PUBLIC_BASE_URL + "/", Duration.ofMinutes(10)));
	}

	@Test
	void 업로드_URL은_R2_엔드포인트의_버킷_키_경로를_가리킨다() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).startsWith(ENDPOINT + "/" + BUCKET + "/" + KEY.value());
	}

	@Test
	void 업로드_URL은_서명과_유효_시간을_담은_presigned_URL이다() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("X-Amz-Signature=");
		assertThat(url.uploadUrl()).contains("X-Amz-Expires=600");
	}

	@Test
	void 서명_대상_헤더에_contentLength와_contentType이_포함된다() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("X-Amz-SignedHeaders=content-length%3Bcontent-type%3Bhost");
	}

	@Test
	void 공개_URL과_객체_키와_유효_시간을_함께_돌려준다() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + KEY.value());
		assertThat(url.objectKey()).isEqualTo(KEY.value());
		assertThat(url.expiresInSeconds()).isEqualTo(600L);
	}
}
