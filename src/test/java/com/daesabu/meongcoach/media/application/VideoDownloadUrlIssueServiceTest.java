package com.daesabu.meongcoach.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.application.provided.VideoDownloadUrlResult;
import com.daesabu.meongcoach.media.application.required.VideoDownloadUrl;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoObjectKeyException;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("영상 다운로드 URL 발급 서비스")
class VideoDownloadUrlIssueServiceTest {

	private static final String OBJECT_KEY = "videos/training/7/550e8400-e29b-41d4-a716-446655440000.mp4";

	private RecordingVideoStorage videoStorage;
	private VideoDownloadUrlIssueService service;

	@BeforeEach
	void setUp() {
		videoStorage = new RecordingVideoStorage();
		service = new VideoDownloadUrlIssueService(videoStorage);
	}

	@Test
	@DisplayName("검증한 객체 키를 스토리지에 넘긴다")
	void issuePassesParsedKeyToStorage() {
		service.issue(OBJECT_KEY);

		assertThat(videoStorage.lastKey().value()).isEqualTo(OBJECT_KEY);
	}

	@Test
	@DisplayName("스토리지가 발급한 URL과 키에서 추출한 소유자 ID를 결과로 반환한다")
	void issueReturnsStorageResultWithOwnerUserId() {
		VideoDownloadUrlResult result = service.issue(OBJECT_KEY);

		assertThat(result.downloadUrl()).isEqualTo("https://storage.test/download");
		assertThat(result.publicUrl()).isEqualTo("https://videos.test/public");
		assertThat(result.s3Uri()).isEqualTo("s3://test-bucket/" + OBJECT_KEY);
		assertThat(result.ownerUserId()).isEqualTo(7L);
		assertThat(result.expiresInSeconds()).isEqualTo(3600L);
	}

	@Test
	@DisplayName("규칙에 어긋나는 객체 키면 발급에 실패한다")
	void issueFailsWhenKeyIsMalformed() {
		assertThatThrownBy(() -> service.issue("images/profile/7/key.png"))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
		assertThat(videoStorage.keys).isEmpty();
	}

	private static class RecordingVideoStorage implements VideoStorage {

		private final List<VideoObjectKey> keys = new ArrayList<>();

		@Override
		public VideoUploadUrl issueUploadUrl(VideoObjectKey key, String contentType, long contentLength) {
			throw new UnsupportedOperationException("다운로드 URL 발급 테스트에서는 쓰지 않는다");
		}

		@Override
		public VideoDownloadUrl issueDownloadUrl(VideoObjectKey key) {
			keys.add(key);
			return new VideoDownloadUrl("https://storage.test/download", "https://videos.test/public",
					"s3://test-bucket/" + key.value(), 3600L);
		}

		private VideoObjectKey lastKey() {
			return keys.getLast();
		}
	}
}
