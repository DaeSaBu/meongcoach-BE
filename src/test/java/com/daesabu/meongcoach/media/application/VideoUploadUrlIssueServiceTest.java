package com.daesabu.meongcoach.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.StoredVideo;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import com.daesabu.meongcoach.media.domain.exception.InvalidUploadTargetException;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedVideoTypeException;
import com.daesabu.meongcoach.media.domain.exception.VideoSizeExceededException;
import com.daesabu.meongcoach.media.domain.vo.VideoFileSize;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("영상 업로드 URL 발급 서비스")
class VideoUploadUrlIssueServiceTest {

	private RecordingVideoStorage videoStorage;
	private VideoUploadUrlIssueService service;

	@BeforeEach
	void setUp() {
		videoStorage = new RecordingVideoStorage();
		service = new VideoUploadUrlIssueService(videoStorage);
	}

	@Test
	@DisplayName("업로드 대상·사용자·영상 형식으로 만든 객체 키를 스토리지에 넘긴다")
	void issuePassesGeneratedKeyToStorage() {
		service.issue(7L, "AI_ANALYSIS", "video/quicktime", 1_024L);

		// 키 포맷 자체는 VideoObjectKeyTest가 검증한다. 여기서는 변환한 값이 그대로 전달되는지만 본다
		assertThat(videoStorage.lastKey().value())
				.startsWith("videos/" + VideoUploadTarget.AI_ANALYSIS.getPathSegment() + "/7/")
				.endsWith("." + VideoType.QUICKTIME.getExtension());
	}

	@Test
	@DisplayName("요청한 파일 크기를 스토리지에 그대로 전달한다")
	void issuePassesContentLengthToStorage() {
		service.issue(7L, "AI_ANALYSIS", "video/mp4", 52_428_800L);

		assertThat(videoStorage.lastContentLength()).isEqualTo(52_428_800L);
	}

	@Test
	@DisplayName("요청한 Content-Type 그대로 스토리지에 전달한다")
	void issuePassesContentTypeToStorage() {
		service.issue(7L, "AI_ANALYSIS", "video/mp4", 1_024L);

		assertThat(videoStorage.lastContentType()).isEqualTo("video/mp4");
	}

	@Test
	@DisplayName("스토리지에 넘긴 객체 키와 스토리지가 발급한 URL을 결과로 반환한다")
	void issueReturnsStorageUrlsWithObjectKey() {
		VideoUploadUrlResult result = service.issue(7L, "AI_ANALYSIS", "video/mp4", 1_024L);

		assertThat(result.objectKey()).isEqualTo(videoStorage.lastKey().value());
		assertThat(result.uploadUrl()).isEqualTo("https://storage.test/upload");
		assertThat(result.publicUrl()).isEqualTo("https://videos.test/public");
		assertThat(result.expiresInSeconds()).isEqualTo(1_800L);
	}

	@Test
	@DisplayName("지원하지 않는 업로드 대상이면 발급에 실패한다")
	void issueFailsWhenTargetIsInvalid() {
		assertThatThrownBy(() -> service.issue(7L, "BANNER", "video/mp4", 1_024L))
				.isInstanceOf(InvalidUploadTargetException.class);
		assertThat(videoStorage.keys).isEmpty();
	}

	@Test
	@DisplayName("지원하지 않는 영상 형식이면 발급에 실패한다")
	void issueFailsWhenContentTypeIsUnsupported() {
		assertThatThrownBy(() -> service.issue(7L, "AI_ANALYSIS", "video/avi", 1_024L))
				.isInstanceOf(UnsupportedVideoTypeException.class);
		assertThat(videoStorage.keys).isEmpty();
	}

	@Test
	@DisplayName("파일 크기가 0이면 발급에 실패한다")
	void issueFailsWhenFileSizeIsZero() {
		assertThatThrownBy(() -> service.issue(7L, "AI_ANALYSIS", "video/mp4", 0L))
				.isInstanceOf(VideoSizeExceededException.class);
		assertThat(videoStorage.keys).isEmpty();
	}

	@Test
	@DisplayName("파일 크기가 상한을 넘으면 발급에 실패한다")
	void issueFailsWhenFileSizeExceedsMax() {
		assertThatThrownBy(() -> service.issue(7L, "AI_ANALYSIS", "video/mp4", VideoFileSize.MAX_BYTES + 1))
				.isInstanceOf(VideoSizeExceededException.class);
		assertThat(videoStorage.keys).isEmpty();
	}

	private static class RecordingVideoStorage implements VideoStorage {

		private final List<VideoObjectKey> keys = new ArrayList<>();
		private final List<String> contentTypes = new ArrayList<>();
		private final List<Long> contentLengths = new ArrayList<>();

		@Override
		public VideoUploadUrl issueUploadUrl(VideoObjectKey key, String contentType, long contentLength) {
			keys.add(key);
			contentTypes.add(contentType);
			contentLengths.add(contentLength);
			return new VideoUploadUrl("https://storage.test/upload", "https://videos.test/public", 1_800L);
		}

		// 발급 유스케이스는 객체 조회를 쓰지 않는다. 완료 확인 쪽은 별도 테스트가 검증한다
		@Override
		public Optional<StoredVideo> findStoredVideo(VideoObjectKey key) {
			return Optional.empty();
		}

		private VideoObjectKey lastKey() {
			return keys.getLast();
		}

		private String lastContentType() {
			return contentTypes.getLast();
		}

		private long lastContentLength() {
			return contentLengths.getLast();
		}
	}
}
