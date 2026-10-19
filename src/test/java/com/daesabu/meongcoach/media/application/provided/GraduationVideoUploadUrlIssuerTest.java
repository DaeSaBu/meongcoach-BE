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
import com.daesabu.meongcoach.media.domain.VideoObjectKey;
import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.UnsupportedVideoTypeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class GraduationVideoUploadUrlIssuerTest {

	private static final long VALID_SIZE = 10L * 1024 * 1024;

	@Autowired
	private VideoStorage videoStorage;

	@Autowired
	private GraduationVideoUploadUrlIssuer service;

	@BeforeEach
	void setUp() {
		given(videoStorage.issueUploadUrl(any(), anyString(), anyLong()))
				.willReturn(new VideoUploadUrl("https://storage.test/upload", "https://videos.test/public",
						"videos/graduation/7/1101/key.mp4", 900L));
	}

	@Test
	void 사용자와_토픽에_해당하는_실습_인증_객체_키를_스토리지에_넘긴다() {
		service.issue(7L, 1101L, "video/mp4", VALID_SIZE);

		assertThat(uploadedKey().value())
				.startsWith("videos/graduation/7/1101/")
				.endsWith("." + VideoType.MP4.getExtension());
	}

	@Test
	void 요청한_contentType_그대로_스토리지에_전달한다() {
		service.issue(7L, 1101L, "video/quicktime", VALID_SIZE);

		verify(videoStorage).issueUploadUrl(any(), eq("video/quicktime"), anyLong());
	}

	@Test
	void 요청한_파일_크기를_스토리지에_전달해_서명에_포함되게_한다() {
		service.issue(7L, 1101L, "video/mp4", VALID_SIZE);

		verify(videoStorage).issueUploadUrl(any(), anyString(), eq(VALID_SIZE));
	}

	@Test
	void 스토리지가_발급한_URL을_결과로_반환한다() {
		VideoUploadUrlResult result = service.issue(7L, 1101L, "video/mp4", VALID_SIZE);

		assertThat(result.uploadUrl()).isEqualTo("https://storage.test/upload");
		assertThat(result.publicUrl()).isEqualTo("https://videos.test/public");
		assertThat(result.objectKey()).isEqualTo("videos/graduation/7/1101/key.mp4");
		assertThat(result.expiresInSeconds()).isEqualTo(900L);
	}

	@Test
	void 지원하지_않는_영상_형식이면_발급에_실패한다() {
		assertThatThrownBy(() -> service.issue(7L, 1101L, "video/webm", VALID_SIZE))
				.isInstanceOf(UnsupportedVideoTypeException.class);
		verifyNoInteractions(videoStorage);
	}

	@ParameterizedTest
	@ValueSource(longs = {52_428_801L, 209_715_200L})
	void AI_영상_상한을_넘어도_실습_인증_상한_이하면_URL을_발급한다(long bytes) {
		service.issue(7L, 1101L, "video/mp4", bytes);

		verify(videoStorage).issueUploadUrl(any(), eq("video/mp4"), eq(bytes));
	}

	@Test
	void 실습_인증_상한_200MiB를_넘으면_발급에_실패한다() {
		assertThatThrownBy(() -> service.issue(7L, 1101L, "video/mp4", 209_715_201L))
				.isInstanceOf(VideoFileSizeExceededException.class);
		verifyNoInteractions(videoStorage);
	}

	@Test
	void 파일_크기가_0_이하면_발급에_실패한다() {
		assertThatThrownBy(() -> service.issue(7L, 1101L, "video/mp4", 0L))
				.isInstanceOf(InvalidVideoFileSizeException.class);
		verifyNoInteractions(videoStorage);
	}

	private VideoObjectKey uploadedKey() {
		ArgumentCaptor<VideoObjectKey> key = ArgumentCaptor.forClass(VideoObjectKey.class);
		verify(videoStorage).issueUploadUrl(key.capture(), anyString(), anyLong());
		return key.getValue();
	}
}
