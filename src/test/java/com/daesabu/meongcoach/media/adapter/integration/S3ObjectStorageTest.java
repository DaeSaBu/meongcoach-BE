package com.daesabu.meongcoach.media.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.application.required.StorageUploadUrl;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class S3ObjectStorageTest {

	private static final String REGION = "ap-northeast-2";
	private static final String BUCKET = "test-video-bucket";
	private static final String PUBLIC_BASE_URL = "https://videos.test.meongcoach.com";
	private static final MediaObjectKey KEY =
			new MediaObjectKey("videos/training/1/550e8400-e29b-41d4-a716-446655440000.mp4");
	private static final long CONTENT_LENGTH = 10L * 1024 * 1024;

	private S3ObjectStorage storage;

	@BeforeEach
	void setUp() {
		storage = new S3ObjectStorage(new S3Properties(REGION, "test-access-key", "test-secret-key",
				BUCKET, PUBLIC_BASE_URL, Duration.ofMinutes(15), Duration.ofHours(1)));
	}

	@Test
	void 업로드_URL은_가상_호스팅_스타일의_버킷_키_경로를_가리킨다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl())
				.startsWith("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/" + KEY.value());
	}

	@Test
	void 업로드_URL은_서명과_유효_시간을_담은_presigned_URL이다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("X-Amz-Signature=");
		assertThat(url.uploadUrl()).contains("X-Amz-Expires=900");
		assertThat(url.expiresInSeconds()).isEqualTo(900L);
	}

	@Test
	void 파일_크기가_있으면_서명_대상_헤더에_contentLength가_포함된다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("X-Amz-SignedHeaders=content-length%3Bcontent-type%3Bhost");
	}

	@Test
	void 파일_크기가_없으면_서명_대상_헤더는_contentType과_host뿐이다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "image/png", null);

		assertThat(url.uploadUrl()).contains("X-Amz-SignedHeaders=content-type%3Bhost");
	}

	@Test
	void 서명_자격_증명은_설정한_리전을_사용한다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("%2F" + REGION + "%2Fs3%2Faws4_request");
	}

	@Test
	void 공개_URL은_공개_도메인_아래의_키_경로다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + KEY.value());
	}

	@Test
	void 다운로드_URL은_서명과_다운로드_전용_유효_시간을_담은_버킷_키_경로의_presigned_URL이다() {
		String url = storage.issueDownloadUrl(KEY);

		assertThat(url)
				.startsWith("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/" + KEY.value())
				.contains("X-Amz-Signature=")
				.contains("X-Amz-Expires=3600");
	}
}
