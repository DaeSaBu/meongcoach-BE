package com.daesabu.meongcoach.media.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.daesabu.meongcoach.media.application.required.StoredVideo;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * presign은 로컬 서명 연산이라 네트워크 없이 실제 S3Presigner로 검증한다.
 * 반면 객체 조회는 실제로 R2를 호출하므로 S3Client를 목으로 대신한다.
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

	private S3Client s3Client;
	private R2VideoStorage storage;

	@BeforeEach
	void setUp() {
		s3Client = mock(S3Client.class);
		storage = new R2VideoStorage(properties(PUBLIC_BASE_URL), s3Client);
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
		R2VideoStorage trailingSlashStorage = new R2VideoStorage(properties(PUBLIC_BASE_URL + "/"), s3Client);

		VideoUploadUrl url = trailingSlashStorage.issueUploadUrl(KEY, CONTENT_TYPE, CONTENT_LENGTH);

		assertThat(url.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + KEY.value());
	}

	@Test
	@DisplayName("유효 시간을 초 단위로 알려준다")
	void expiresInSecondsMatchesVideoValidity() {
		VideoUploadUrl url = storage.issueUploadUrl(KEY, CONTENT_TYPE, CONTENT_LENGTH);

		assertThat(url.expiresInSeconds()).isEqualTo(1800L);
	}

	@Test
	@DisplayName("설정만 받는 생성자는 S3Client를 스스로 구성한다")
	void publicConstructorBuildsOwnS3Client() {
		R2VideoStorage selfConfigured = new R2VideoStorage(properties(PUBLIC_BASE_URL));

		// 객체 조회는 네트워크가 필요해 확인할 수 없으므로 구성이 끝난 뒤 발급 경로가 그대로 동작하는지만 본다
		assertThat(selfConfigured.issueUploadUrl(KEY, CONTENT_TYPE, CONTENT_LENGTH).uploadUrl())
				.contains("X-Amz-Signature=");
	}

	@Test
	@DisplayName("저장된 영상의 Content-Type과 실제 크기를 담아 돌려준다")
	void findStoredVideoReturnsContentTypeAndSize() {
		given(s3Client.headObject(any(HeadObjectRequest.class))).willReturn(HeadObjectResponse.builder()
				.contentType(CONTENT_TYPE)
				.contentLength(CONTENT_LENGTH)
				.build());

		Optional<StoredVideo> found = storage.findStoredVideo(KEY);

		assertThat(found).contains(new StoredVideo(CONTENT_TYPE, CONTENT_LENGTH));
	}

	@Test
	@DisplayName("조회 요청에 설정된 버킷과 객체 키를 담는다")
	void findStoredVideoSendsBucketAndKey() {
		given(s3Client.headObject(any(HeadObjectRequest.class))).willReturn(HeadObjectResponse.builder()
				.contentType(CONTENT_TYPE)
				.contentLength(CONTENT_LENGTH)
				.build());

		storage.findStoredVideo(KEY);

		ArgumentCaptor<HeadObjectRequest> captor = ArgumentCaptor.forClass(HeadObjectRequest.class);
		then(s3Client).should().headObject(captor.capture());
		assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
		assertThat(captor.getValue().key()).isEqualTo(KEY.value());
	}

	@Test
	@DisplayName("객체가 없으면 빈 값을 돌려준다")
	void findStoredVideoReturnsEmptyWhenKeyIsMissing() {
		willThrow(NoSuchKeyException.builder().statusCode(404).message("Not Found").build())
				.given(s3Client).headObject(any(HeadObjectRequest.class));

		assertThat(storage.findStoredVideo(KEY)).isEmpty();
	}

	@Test
	@DisplayName("없는 객체에 403이 와도 업로드되지 않은 것으로 본다")
	void findStoredVideoReturnsEmptyOnForbidden() {
		// R2는 토큰 권한에 따라 없는 객체에 404 대신 403을 주기도 한다
		willThrow(S3Exception.builder().statusCode(403).message("Forbidden").build())
				.given(s3Client).headObject(any(HeadObjectRequest.class));

		assertThat(storage.findStoredVideo(KEY)).isEmpty();
	}

	@Test
	@DisplayName("스토리지 장애는 없는 것으로 삼키지 않고 그대로 전파한다")
	void findStoredVideoPropagatesServerError() {
		willThrow(S3Exception.builder().statusCode(500).message("Internal Error").build())
				.given(s3Client).headObject(any(HeadObjectRequest.class));

		assertThatThrownBy(() -> storage.findStoredVideo(KEY))
				.isInstanceOf(S3Exception.class);
	}

	private static R2Properties properties(String publicBaseUrl) {
		return new R2Properties(ENDPOINT, "test-access-key", "test-secret-key", BUCKET, publicBaseUrl,
				Duration.ofMinutes(10), Duration.ofMinutes(30));
	}
}
