package com.daesabu.meongcoach.media.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("영상 객체 키 값 객체")
class VideoObjectKeyTest {

	@Test
	@DisplayName("훈련 영상의 객체 키는 대상·사용자 ID·확장자를 담는다")
	void createBuildsTrainingVideoKey() {
		VideoObjectKey key = VideoObjectKey.create(VideoUploadTarget.TRAINING_VIDEO, 7L, VideoType.MP4);

		assertThat(key.value()).matches("videos/training/7/[0-9a-f-]{36}\\.mp4");
	}

	@Test
	@DisplayName("quicktime 영상은 mov 확장자를 쓴다")
	void createUsesMovExtensionForQuickTime() {
		VideoObjectKey key = VideoObjectKey.create(VideoUploadTarget.TRAINING_VIDEO, 7L, VideoType.MOV);

		assertThat(key.value()).matches("videos/training/7/[0-9a-f-]{36}\\.mov");
	}

	@Test
	@DisplayName("생성할 때마다 서로 다른 객체 키를 만든다")
	void createGeneratesUniqueKeys() {
		VideoObjectKey first = VideoObjectKey.create(VideoUploadTarget.TRAINING_VIDEO, 7L, VideoType.MP4);
		VideoObjectKey second = VideoObjectKey.create(VideoUploadTarget.TRAINING_VIDEO, 7L, VideoType.MP4);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	@DisplayName("같은 값끼리는 동등하다")
	void sameValuesAreEqual() {
		assertThat(new VideoObjectKey("videos/training/7/key.mp4"))
				.isEqualTo(new VideoObjectKey("videos/training/7/key.mp4"));
	}
}
