package com.daesabu.meongcoach.media.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.application.required.StorageUploadUrl;
import com.daesabu.meongcoach.media.domain.MediaObjectKey;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GcsObjectStorageTest {

	private static final String PROJECT_ID = "test-gcp-project";
	private static final String BUCKET = "test-ai-video-bucket";
	private static final String BASE_URL = "https://storage.googleapis.com/" + BUCKET + "/";
	private static final MediaObjectKey KEY =
			new MediaObjectKey("videos/training/1/550e8400-e29b-41d4-a716-446655440000.mp4");
	private static final long CONTENT_LENGTH = 10L * 1024 * 1024;

	private GcsObjectStorage storage;

	@BeforeEach
	void setUp() throws NoSuchAlgorithmException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		ServiceAccountCredentials credentials = ServiceAccountCredentials.newBuilder()
				.setProjectId(PROJECT_ID)
				.setClientEmail("test-api@" + PROJECT_ID + ".iam.gserviceaccount.com")
				.setPrivateKeyId("test-private-key-id")
				.setPrivateKey(keyPair.getPrivate())
				.build();
		Storage googleStorage = StorageOptions.newBuilder()
				.setProjectId(PROJECT_ID)
				.setCredentials(credentials)
				.build()
				.getService();
		storage = new GcsObjectStorage(new GcsProperties(BUCKET, Duration.ofMinutes(15), Duration.ofHours(1)),
				googleStorage);
	}

	@Test
	void 업로드_URL은_버킷_표준_도메인의_버킷_키_경로를_가리킨다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).startsWith(BASE_URL + KEY.value());
	}

	@Test
	void 업로드_URL은_서명과_유효_시간을_담은_V4_서명_URL이다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("X-Goog-Signature=");
		assertThat(url.uploadUrl()).contains("X-Goog-Expires=900");
		assertThat(url.expiresInSeconds()).isEqualTo(900L);
	}

	@Test
	void 파일_크기가_있으면_서명_대상_헤더에_contentLength가_포함된다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.uploadUrl()).contains("X-Goog-SignedHeaders=content-length%3Bcontent-type%3Bhost");
	}

	@Test
	void 파일_크기가_없으면_서명_대상_헤더는_contentType과_host뿐이다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", null);

		assertThat(url.uploadUrl()).contains("X-Goog-SignedHeaders=content-type%3Bhost");
	}

	@Test
	void 공개_URL은_버킷_표준_도메인_아래의_키_경로다() {
		StorageUploadUrl url = storage.issueUploadUrl(KEY, "video/mp4", CONTENT_LENGTH);

		assertThat(url.publicUrl()).isEqualTo(BASE_URL + KEY.value());
	}

	@Test
	void 다운로드_URL은_서명과_다운로드_전용_유효_시간을_담은_버킷_키_경로의_V4_서명_URL이다() {
		String url = storage.issueDownloadUrl(KEY);

		assertThat(url)
				.startsWith(BASE_URL + KEY.value())
				.contains("X-Goog-Signature=")
				.contains("X-Goog-Expires=3600");
	}

	@Test
	void 스토리지_URI는_gs_스킴의_버킷_키_경로다() {
		assertThat(storage.storageUri(KEY)).isEqualTo("gs://" + BUCKET + "/" + KEY.value());
	}
}
