package com.daesabu.meongcoach.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoUploadTargetException;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedVideoTypeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
import com.daesabu.meongcoach.media.domain.vo.VideoFileSize;
import com.daesabu.meongcoach.media.domain.vo.VideoObjectKey;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("영상 업로드 URL 발급 서비스")
class VideoUploadUrlIssueServiceTest {

	private static final long VALID_SIZE = 10L * 1024 * 1024;

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
		service.issue(7L, "TRAINING_VIDEO", "video/mp4", VALID_SIZE);

		// 키 포맷 자체는 VideoObjectKeyTest가 검증한다. 여기서는 변환한 값이 그대로 전달되는지만 본다
		assertThat(videoStorage.lastKey().value())
				.startsWith("videos/" + VideoUploadTarget.TRAINING_VIDEO.getPathSegment() + "/7/")
				.endsWith("." + VideoType.MP4.getExtension());
	}

	@Test
	@DisplayName("요청한 Content-Type 그대로 스토리지에 전달한다")
	void issuePassesContentTypeToStorage() {
		service.issue(7L, "TRAINING_VIDEO", "video/quicktime", VALID_SIZE);

		assertThat(videoStorage.lastContentType()).isEqualTo("video/quicktime");
	}

	@Test
	@DisplayName("요청한 파일 크기를 스토리지에 전달해 서명에 포함되게 한다")
	void issuePassesFileSizeToStorage() {
		service.issue(7L, "TRAINING_VIDEO", "video/mp4", VALID_SIZE);

		assertThat(videoStorage.lastContentLength()).isEqualTo(VALID_SIZE);
	}

	@Test
	@DisplayName("스토리지가 발급한 URL을 결과로 반환한다")
	void issueReturnsStorageResult() {
		VideoUploadUrlResult result = service.issue(7L, "TRAINING_VIDEO", "video/mp4", VALID_SIZE);

		assertThat(result.uploadUrl()).isEqualTo("https://storage.test/upload");
		assertThat(result.publicUrl()).isEqualTo("https://videos.test/public");
		assertThat(result.objectKey()).isEqualTo("videos/training/7/key.mp4");
		assertThat(result.expiresInSeconds()).isEqualTo(900L);
	}

	@Test
	@DisplayName("지원하지 않는 업로드 대상이면 발급에 실패한다")
	void issueFailsWhenTargetIsInvalid() {
		assertThatThrownBy(() -> service.issue(7L, "USER_PROFILE", "video/mp4", VALID_SIZE))
				.isInstanceOf(InvalidVideoUploadTargetException.class);
		assertThat(videoStorage.keys).isEmpty();
	}

	@Test
	@DisplayName("지원하지 않는 영상 형식이면 발급에 실패한다")
	void issueFailsWhenContentTypeIsUnsupported() {
		assertThatThrownBy(() -> service.issue(7L, "TRAINING_VIDEO", "video/webm", VALID_SIZE))
				.isInstanceOf(UnsupportedVideoTypeException.class);
		assertThat(videoStorage.keys).isEmpty();
	}

	@Test
	@DisplayName("상한을 넘는 파일 크기면 발급에 실패한다")
	void issueFailsWhenFileSizeExceedsMax() {
		assertThatThrownBy(() -> service.issue(7L, "TRAINING_VIDEO", "video/mp4", VideoFileSize.MAX_BYTES + 1))
				.isInstanceOf(VideoFileSizeExceededException.class);
		assertThat(videoStorage.keys).isEmpty();
	}

	@Test
	@DisplayName("0 이하의 파일 크기면 발급에 실패한다")
	void issueFailsWhenFileSizeIsNotPositive() {
		assertThatThrownBy(() -> service.issue(7L, "TRAINING_VIDEO", "video/mp4", 0L))
				.isInstanceOf(InvalidVideoFileSizeException.class);
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
			return new VideoUploadUrl("https://storage.test/upload", "https://videos.test/public",
					"videos/training/7/key.mp4", 900L);
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
