package com.daesabu.meongcoach.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;
import com.daesabu.meongcoach.media.application.required.VideoDownloadUrl;
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
import org.junit.jupiter.api.Test;

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
	void 업로드_대상_사용자_영상_형식으로_만든_객체_키를_스토리지에_넘긴다() {
		service.issue(7L, "TRAINING_VIDEO", "video/mp4", VALID_SIZE);

		// 키 포맷 자체는 VideoObjectKeyTest가 검증한다. 여기서는 변환한 값이 그대로 전달되는지만 본다
		assertThat(videoStorage.lastKey().value())
				.startsWith("videos/" + VideoUploadTarget.TRAINING_VIDEO.getPathSegment() + "/7/")
				.endsWith("." + VideoType.MP4.getExtension());
	}

	@Test
	void 요청한_contentType_그대로_스토리지에_전달한다() {
		service.issue(7L, "TRAINING_VIDEO", "video/quicktime", VALID_SIZE);

		assertThat(videoStorage.lastContentType()).isEqualTo("video/quicktime");
	}

	@Test
	void 요청한_파일_크기를_스토리지에_전달해_서명에_포함되게_한다() {
		service.issue(7L, "TRAINING_VIDEO", "video/mp4", VALID_SIZE);

		assertThat(videoStorage.lastContentLength()).isEqualTo(VALID_SIZE);
	}

	@Test
	void 스토리지가_발급한_URL을_결과로_반환한다() {
		VideoUploadUrlResult result = service.issue(7L, "TRAINING_VIDEO", "video/mp4", VALID_SIZE);

		assertThat(result.uploadUrl()).isEqualTo("https://storage.test/upload");
		assertThat(result.publicUrl()).isEqualTo("https://videos.test/public");
		assertThat(result.objectKey()).isEqualTo("videos/training/7/key.mp4");
		assertThat(result.expiresInSeconds()).isEqualTo(900L);
	}

	@Test
	void 지원하지_않는_업로드_대상이면_발급에_실패한다() {
		assertThatThrownBy(() -> service.issue(7L, "USER_PROFILE", "video/mp4", VALID_SIZE))
				.isInstanceOf(InvalidVideoUploadTargetException.class);
		assertThat(videoStorage.keys).isEmpty();
	}

	@Test
	void 지원하지_않는_영상_형식이면_발급에_실패한다() {
		assertThatThrownBy(() -> service.issue(7L, "TRAINING_VIDEO", "video/webm", VALID_SIZE))
				.isInstanceOf(UnsupportedVideoTypeException.class);
		assertThat(videoStorage.keys).isEmpty();
	}

	@Test
	void 상한을_넘는_파일_크기면_발급에_실패한다() {
		assertThatThrownBy(() -> service.issue(7L, "TRAINING_VIDEO", "video/mp4", VideoFileSize.MAX_BYTES + 1))
				.isInstanceOf(VideoFileSizeExceededException.class);
		assertThat(videoStorage.keys).isEmpty();
	}

	@Test
	void 파일_크기가_0_이하면_발급에_실패한다() {
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

		@Override
		public VideoDownloadUrl issueDownloadUrl(VideoObjectKey key) {
			throw new UnsupportedOperationException("업로드 URL 발급 테스트에서는 쓰지 않는다");
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
