package com.daesabu.meongcoach.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.application.provided.VerifiedVideoResult;
import com.daesabu.meongcoach.media.application.required.StoredVideo;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.exception.InvalidObjectKeyException;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedVideoTypeException;
import com.daesabu.meongcoach.media.domain.exception.VideoAccessDeniedException;
import com.daesabu.meongcoach.media.domain.exception.VideoNotUploadedException;
import com.daesabu.meongcoach.media.domain.exception.VideoSizeExceededException;
import com.daesabu.meongcoach.media.domain.vo.VideoFileSize;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("영상 업로드 완료 확인 서비스")
class VideoUploadVerifyServiceTest {

	private static final Long OWNER_ID = 7L;
	private static final String OBJECT_KEY = "videos/ai-analysis/7/550e8400-e29b-41d4-a716-446655440000.mp4";
	private static final String PUBLIC_BASE_URL = "https://videos.test";

	private RecordingVideoStorage videoStorage;
	private VideoUploadVerifyService service;

	@BeforeEach
	void setUp() {
		videoStorage = new RecordingVideoStorage();
		service = new VideoUploadVerifyService(videoStorage);
	}

	@Test
	@DisplayName("스토리지가 보고한 형식과 크기를 담아 확인 결과를 반환한다")
	void verifyReturnsStoredVideoInformation() {
		videoStorage.store(new StoredVideo("video/mp4", 52_428_800L));

		VerifiedVideoResult result = service.verify(OWNER_ID, OBJECT_KEY);

		assertThat(result.objectKey()).isEqualTo(OBJECT_KEY);
		assertThat(result.publicUrl()).isEqualTo(PUBLIC_BASE_URL + "/" + OBJECT_KEY);
		assertThat(result.contentType()).isEqualTo("video/mp4");
		assertThat(result.sizeBytes()).isEqualTo(52_428_800L);
	}

	@Test
	@DisplayName("객체 키 형식이 올바르지 않으면 확인에 실패한다")
	void verifyFailsWhenObjectKeyIsMalformed() {
		assertThatThrownBy(() -> service.verify(OWNER_ID, "videos/ai-analysis/7/broken.avi"))
				.isInstanceOf(InvalidObjectKeyException.class);
		assertThat(videoStorage.lookedUpKeys).isEmpty();
	}

	@Test
	@DisplayName("다른 사용자의 객체 키면 스토리지를 조회하지 않고 확인에 실패한다")
	void verifyFailsWithoutStorageLookupWhenKeyBelongsToAnotherUser() {
		videoStorage.store(new StoredVideo("video/mp4", 1_024L));

		assertThatThrownBy(() -> service.verify(99L, OBJECT_KEY))
				.isInstanceOf(VideoAccessDeniedException.class);
		// 조회 자체를 하지 않아야 남의 객체가 존재하는지를 응답 차이로 유추당하지 않는다
		assertThat(videoStorage.lookedUpKeys).isEmpty();
	}

	@Test
	@DisplayName("스토리지에 객체가 없으면 확인에 실패한다")
	void verifyFailsWhenVideoIsNotUploaded() {
		assertThatThrownBy(() -> service.verify(OWNER_ID, OBJECT_KEY))
				.isInstanceOf(VideoNotUploadedException.class);
		assertThat(videoStorage.lookedUpKeys).hasSize(1);
	}

	@Test
	@DisplayName("실제 저장된 형식이 허용 목록 밖이면 확인에 실패한다")
	void verifyFailsWhenStoredContentTypeIsUnsupported() {
		videoStorage.store(new StoredVideo("video/avi", 1_024L));

		assertThatThrownBy(() -> service.verify(OWNER_ID, OBJECT_KEY))
				.isInstanceOf(UnsupportedVideoTypeException.class);
	}

	@Test
	@DisplayName("실제 저장된 크기가 상한을 넘으면 확인에 실패한다")
	void verifyFailsWhenStoredSizeExceedsMax() {
		videoStorage.store(new StoredVideo("video/mp4", VideoFileSize.MAX_BYTES + 1));

		assertThatThrownBy(() -> service.verify(OWNER_ID, OBJECT_KEY))
				.isInstanceOf(VideoSizeExceededException.class);
	}

	private static class RecordingVideoStorage implements VideoStorage {

		private final List<VideoObjectKey> lookedUpKeys = new ArrayList<>();
		private Optional<StoredVideo> storedVideo = Optional.empty();

		// 완료 확인 유스케이스는 URL 발급을 쓰지 않는다. 발급 경로는 VideoUploadUrlIssueServiceTest가 검증한다
		@Override
		public VideoUploadUrl issueUploadUrl(VideoObjectKey key, String contentType, long contentLength) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<StoredVideo> findStoredVideo(VideoObjectKey key) {
			lookedUpKeys.add(key);
			return storedVideo;
		}

		@Override
		public String publicUrlOf(VideoObjectKey key) {
			return PUBLIC_BASE_URL + "/" + key.value();
		}

		private void store(StoredVideo video) {
			storedVideo = Optional.of(video);
		}
	}
}
