package com.daesabu.meongcoach.media.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.application.required.StorageUploadUrl;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class R2ObjectStorageTest {

	private static final String ENDPOINT = "https://test-account.r2.cloudflarestorage.com";
	private static final String BUCKET = "test-bucket";
	private static final String PUBLIC_BASE_URL = "https://images.test.meongcoach.com";
	private static final MediaObjectKey KEY =
			new MediaObjectKey("images/user-profile/1/550e8400-e29b-41d4-a716-446655440000.jpg");
	private static final long CONTENT_LENGTH = 10L * 1024 * 1024;

	private R2ObjectStorage storage;

	@BeforeEach
	void setUp() {
		storage = new R2ObjectStorage(new R2Properties(ENDPOINT, "test-access-key", "test-secret-key",
				BUCKET, PUBLIC_BASE_URL + "/", Duration.ofMinutes(10)));
	}

	@Test
	void 업로드_URL은_R2_엔드포인트의_버킷_키_경로를_가리킨다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg", null);

		assertThat(url.uploadUrl()).startsWith(ENDPOINT + "/" + BUCKET + "/" + KEY.value());
	}

	@Test
	void 업로드_URL은_서명과_유효_시간을_담은_presigned_URL이다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg", null);

		assertThat(url.uploadUrl()).contains("X-Amz-Signature=");
		assertThat(url.uploadUrl()).contains("X-Amz-Expires=600");
		assertThat(url.expiresInSeconds()).isEqualTo(600L);
	}

	@Test
	void 파일_크기가_없으면_서명_대상_헤더는_contentType과_host뿐이다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg", null);

		assertThat(url.uploadUrl()).contains("X-Amz-SignedHeaders=content-type%3Bhost");
	}

	@Test
	void 파일_크기가_있으면_서명_대상_헤더에_contentLength가_포함된다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("X-Amz-SignedHeaders=content-length%3Bcontent-type%3Bhost");
	}

	@Test
	void 공개_URL은_공개_도메인_아래의_키_경로다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "image/jpeg", null);

		assertThat(url.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + KEY.value());
	}

	@Test
	void 공개_도메인_아래의_URL에서_객체_키를_꺼낸다() {
		assertThat(storage.objectKeyOf(PUBLIC_BASE_URL + "/" + KEY.value())).contains(KEY);
	}

	@Test
	void 공개_도메인_아래가_아닌_URL에서는_객체_키를_꺼내지_않는다() {
		assertThat(storage.objectKeyOf("https://evil.example.com/" + KEY.value())).isEmpty();
		assertThat(storage.objectKeyOf(PUBLIC_BASE_URL + ".evil.example.com/" + KEY.value())).isEmpty();
		assertThat(storage.objectKeyOf(null)).isEmpty();
	}
}
