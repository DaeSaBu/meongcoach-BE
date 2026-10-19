package com.daesabu.meongcoach.media.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.daesabu.meongcoach.media.application.required.VideoStorage;
import com.daesabu.meongcoach.media.application.required.VideoUploadUrl;
import com.daesabu.meongcoach.media.domain.VideoFileSize;
import com.daesabu.meongcoach.media.domain.VideoObjectKey;
import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoUploadTargetException;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedVideoTypeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class VideoUploadUrlIssuerTest {

	private static final long VALID_SIZE = 10L * 1024 * 1024;

	@Autowired
	private VideoStorage videoStorage;

	@Autowired
	private VideoUploadUrlIssuer service;

	@BeforeEach
	void setUp() {
		given(videoStorage.issueUploadUrl(any(), anyString(), anyLong()))
				.willReturn(new VideoUploadUrl("https://storage.test/upload", "https://videos.test/public",
						"videos/training/7/key.mp4", 900L));
	}

	@Test
	void 업로드_대상_사용자_영상_형식으로_만든_객체_키를_스토리지에_넘긴다() {
		service.issue(7L, "TRAINING_VIDEO", "video/mp4", VALID_SIZE);

		assertThat(uploadedKey().value())
				.startsWith("videos/" + VideoUploadTarget.TRAINING_VIDEO.getPathSegment() + "/7/")
				.endsWith("." + VideoType.MP4.getExtension());
	}

	@Test
	void 요청한_contentType_그대로_스토리지에_전달한다() {
		service.issue(7L, "TRAINING_VIDEO", "video/quicktime", VALID_SIZE);

		verify(videoStorage).issueUploadUrl(any(), eq("video/quicktime"), anyLong());
	}

	@Test
	void 요청한_파일_크기를_스토리지에_전달해_서명에_포함되게_한다() {
		service.issue(7L, "TRAINING_VIDEO", "video/mp4", VALID_SIZE);

		verify(videoStorage).issueUploadUrl(any(), anyString(), eq(VALID_SIZE));
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
		verifyNoInteractions(videoStorage);
	}

	@Test
	void 지원하지_않는_영상_형식이면_발급에_실패한다() {
		assertThatThrownBy(() -> service.issue(7L, "TRAINING_VIDEO", "video/webm", VALID_SIZE))
				.isInstanceOf(UnsupportedVideoTypeException.class);
		verifyNoInteractions(videoStorage);
	}

	@Test
	void 상한을_넘는_파일_크기면_발급에_실패한다() {
		assertThatThrownBy(() -> service.issue(7L, "TRAINING_VIDEO", "video/mp4", VideoFileSize.MAX_BYTES + 1))
				.isInstanceOf(VideoFileSizeExceededException.class);
		verifyNoInteractions(videoStorage);
	}

	@Test
	void 파일_크기가_0_이하면_발급에_실패한다() {
		assertThatThrownBy(() -> service.issue(7L, "TRAINING_VIDEO", "video/mp4", 0L))
				.isInstanceOf(InvalidVideoFileSizeException.class);
		verifyNoInteractions(videoStorage);
	}

	private VideoObjectKey uploadedKey() {
		ArgumentCaptor<VideoObjectKey> key = ArgumentCaptor.forClass(VideoObjectKey.class);
		verify(videoStorage).issueUploadUrl(key.capture(), anyString(), anyLong());
		return key.getValue();
	}
}
