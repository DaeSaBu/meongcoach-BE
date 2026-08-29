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
import org.junit.jupiter.api.Test;

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
	void 검증한_객체_키를_스토리지에_넘긴다() {
		service.issue(OBJECT_KEY);

		assertThat(videoStorage.lastKey().value()).isEqualTo(OBJECT_KEY);
	}

	@Test
	void 스토리지가_발급한_URL과_키에서_추출한_소유자_ID를_결과로_반환한다() {
		VideoDownloadUrlResult result = service.issue(OBJECT_KEY);

		assertThat(result.downloadUrl()).isEqualTo("https://storage.test/download");
		assertThat(result.publicUrl()).isEqualTo("https://videos.test/public");
		assertThat(result.ownerUserId()).isEqualTo(7L);
		assertThat(result.expiresInSeconds()).isEqualTo(3600L);
	}

	@Test
	void 규칙에_어긋나는_객체_키면_발급에_실패한다() {
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
			return new VideoDownloadUrl("https://storage.test/download", "https://videos.test/public", 3600L);
		}

		private VideoObjectKey lastKey() {
			return keys.getLast();
		}
	}
}
