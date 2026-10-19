package com.daesabu.meongcoach.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoObjectKeyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VideoObjectKeyTest {

	@Test
	void 훈련_영상의_객체_키는_대상_사용자_ID_확장자를_담는다() {
		VideoObjectKey key = VideoObjectKey.create(VideoUploadTarget.TRAINING_VIDEO, 7L, VideoType.MP4);

		assertThat(key.value()).matches("videos/training/7/[0-9a-f-]{36}\\.mp4");
		assertThat(VideoObjectKey.parse(key.value())).isEqualTo(key);
	}

	@Test
	void 실습_인증_영상의_객체_키는_사용자_ID와_토픽_ID를_담는다() {
		VideoObjectKey key = VideoObjectKey.createGraduation(7L, 1101L, VideoType.MP4);

		assertThat(key.value()).matches("videos/graduation/7/1101/[0-9a-f-]{36}\\.mp4");
		assertThat(VideoObjectKey.parse(key.value())).isEqualTo(key);
	}

	@Test
	void 실습_인증_영상은_토픽_ID가_없으면_만들지_않는다() {
		assertThatThrownBy(() -> VideoObjectKey.createGraduation(7L, null, VideoType.MP4))
				.isInstanceOf(IllegalArgumentException.class);
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
	void 묶음_ID가_있는_키_문자열도_값_객체로_만들고_소유자를_추출한다() {
		VideoObjectKey key = VideoObjectKey.parse("videos/graduation/42/1101/550e8400-e29b-41d4-a716-446655440000.mp4");

		assertThat(key.ownerUserId()).isEqualTo(42L);
	}

	@Test
	void 키_경로에서_소유자_사용자_ID를_추출한다() {
		VideoObjectKey key = VideoObjectKey.parse("videos/training/42/key.mp4");

		assertThat(key.ownerUserId()).isEqualTo(42L);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"videos/training/7/1101/key.mp4",
			"videos/graduation/7/key.mp4",
			"videos/graduation/7/1101/42/key.mp4",
			"videos/other/7/key.mp4",
			"videos/other/7/1101/key.mp4"
	})
	void 대상별_경로_형식에_맞지_않는_객체_키는_거부한다(String value) {
		assertThatThrownBy(() -> VideoObjectKey.parse(value))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
	}

	@Test
	void 규칙에_어긋나는_키_문자열은_거부한다() {
		assertThatThrownBy(() -> VideoObjectKey.parse("images/profile/7/key.png"))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/training/not-a-number/key.mp4"))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/training/7/no-extension"))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/graduation/7/not-a-number/key.mp4"))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
		assertThatThrownBy(() -> VideoObjectKey.parse(null))
				.isInstanceOf(InvalidVideoObjectKeyException.class);
	}
}
