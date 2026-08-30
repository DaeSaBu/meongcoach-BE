package com.daesabu.meongcoach.media.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import com.daesabu.meongcoach.media.domain.exception.InvalidVideoObjectKeyException;
import org.junit.jupiter.api.Test;

class VideoObjectKeyTest {

	@Test
	void 훈련_영상의_객체_키는_대상_사용자_ID_확장자를_담는다() {
		VideoObjectKey key = VideoObjectKey.create(VideoUploadTarget.TRAINING_VIDEO, 7L, VideoType.MP4);

		assertThat(key.value()).matches("videos/training/7/[0-9a-f-]{36}\\.mp4");
	}

	@Test
	void quicktime_영상은_mov_확장자를_쓴다() {
		VideoObjectKey key = VideoObjectKey.create(VideoUploadTarget.TRAINING_VIDEO, 7L, VideoType.MOV);

		assertThat(key.value()).matches("videos/training/7/[0-9a-f-]{36}\\.mov");
	}

	@Test
	void 생성할_때마다_서로_다른_객체_키를_만든다() {
		VideoObjectKey first = VideoObjectKey.create(VideoUploadTarget.TRAINING_VIDEO, 7L, VideoType.MP4);
		VideoObjectKey second = VideoObjectKey.create(VideoUploadTarget.TRAINING_VIDEO, 7L, VideoType.MP4);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void 같은_값끼리는_동등하다() {
		assertThat(new VideoObjectKey("videos/training/7/key.mp4"))
				.isEqualTo(new VideoObjectKey("videos/training/7/key.mp4"));
	}

	@Test
	void 규칙에_맞는_키_문자열을_값_객체로_만든다() {
		VideoObjectKey key = VideoObjectKey.parse("videos/training/7/550e8400-e29b-41d4-a716-446655440000.mp4");

		assertThat(key.value()).isEqualTo("videos/training/7/550e8400-e29b-41d4-a716-446655440000.mp4");
	}

	@Test
	void 키_경로에서_소유자_사용자_ID를_추출한다() {
		VideoObjectKey key = VideoObjectKey.parse("videos/training/42/key.mp4");

		assertThat(key.ownerUserId()).isEqualTo(42L);
	}

	@Test
	void 규칙에_어긋나는_키_문자열은_거부한다() {
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
