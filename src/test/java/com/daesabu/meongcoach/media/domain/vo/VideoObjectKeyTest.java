package com.daesabu.meongcoach.media.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoObjectKeyException;
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

	@Test
	@DisplayName("규칙에 맞는 키 문자열을 값 객체로 만든다")
	void parseAcceptsWellFormedKey() {
		VideoObjectKey key = VideoObjectKey.parse("videos/training/7/550e8400-e29b-41d4-a716-446655440000.mp4");

		assertThat(key.value()).isEqualTo("videos/training/7/550e8400-e29b-41d4-a716-446655440000.mp4");
	}

	@Test
	@DisplayName("키 경로에서 소유자 사용자 ID를 추출한다")
	void ownerUserIdComesFromKeyPath() {
		VideoObjectKey key = VideoObjectKey.parse("videos/training/42/key.mp4");

		assertThat(key.ownerUserId()).isEqualTo(42L);
	}

	@Test
	@DisplayName("영상 키에서 썸네일 키를 유도한다")
	void thumbnailKeyIsDerivedFromVideoKey() {
		VideoObjectKey key = VideoObjectKey.parse("videos/training/7/550e8400-e29b-41d4-a716-446655440000.mp4");

		assertThat(key.thumbnailKey()).isEqualTo("thumbnails/training/7/550e8400-e29b-41d4-a716-446655440000.jpg");
	}

	@Test
	@DisplayName("mov 영상도 jpg 썸네일 키를 유도한다")
	void thumbnailKeyUsesJpgExtensionForMov() {
		VideoObjectKey key = VideoObjectKey.parse("videos/training/7/key.mov");

		assertThat(key.thumbnailKey()).isEqualTo("thumbnails/training/7/key.jpg");
	}

	@Test
	@DisplayName("규칙에 어긋나는 키 문자열은 거부한다")
	void parseRejectsMalformedKey() {
		assertThatThrownBy(() -> VideoObjectKey.parse("images/profile/7/key.png"))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/training/not-a-number/key.mp4"))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/training/7/no-extension"))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
		assertThatThrownBy(() -> VideoObjectKey.parse(null))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
	}
}
