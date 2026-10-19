package com.daesabu.meongcoach.graduation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.domain.exception.InvalidGraduationVideoKeyException;
import org.junit.jupiter.api.Test;

class GraduationVideoKeyTest {

	private static final Long USER_ID = 42L;
	private static final Long TOPIC_ID = 7L;

	@Test
	void 본인이_같은_토픽으로_올린_실습_영상_키를_받아들인다() {
		GraduationVideoKey key = GraduationVideoKey.of("videos/graduation/42/7/550e8400.mp4", USER_ID, TOPIC_ID);

		assertThat(key.value()).isEqualTo("videos/graduation/42/7/550e8400.mp4");
	}

	@Test
	void 다른_사용자의_실습_영상_키는_거절한다() {
		assertThatThrownBy(() -> GraduationVideoKey.of("videos/graduation/43/7/550e8400.mp4", USER_ID, TOPIC_ID))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
	}

	@Test
	void 다른_토픽으로_올린_실습_영상_키는_거절한다() {
		assertThatThrownBy(() -> GraduationVideoKey.of("videos/graduation/42/8/550e8400.mp4", USER_ID, TOPIC_ID))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
	}

	@Test
	void 토픽_단계가_없는_키는_거절한다() {
		assertThatThrownBy(() -> GraduationVideoKey.of("videos/graduation/42/550e8400.mp4", USER_ID, TOPIC_ID))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
	}

	@Test
	void 다른_용도의_영상_키는_거절한다() {
		assertThatThrownBy(() -> GraduationVideoKey.of("videos/training/42/7/550e8400.mp4", USER_ID, TOPIC_ID))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
	}

	@Test
	void 형식에_어긋난_키는_거절한다() {
		assertThatThrownBy(() -> GraduationVideoKey.of("videos/graduation/42/7/no-extension", USER_ID, TOPIC_ID))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
		assertThatThrownBy(() -> GraduationVideoKey.of("videos/graduation/42/7/../8/x.mp4", USER_ID, TOPIC_ID))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
		assertThatThrownBy(() -> GraduationVideoKey.of(null, USER_ID, TOPIC_ID))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
	}
}
