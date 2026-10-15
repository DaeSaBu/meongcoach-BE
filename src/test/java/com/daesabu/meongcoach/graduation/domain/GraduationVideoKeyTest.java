package com.daesabu.meongcoach.graduation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.domain.exception.InvalidGraduationVideoKeyException;
import org.junit.jupiter.api.Test;

class GraduationVideoKeyTest {

	@Test
	void 본인이_올린_실습_영상_키를_받아들인다() {
		GraduationVideoKey key = GraduationVideoKey.of("videos/graduation/42/550e8400.mp4", 42L);

		assertThat(key.value()).isEqualTo("videos/graduation/42/550e8400.mp4");
	}

	@Test
	void 다른_사용자의_실습_영상_키는_거절한다() {
		assertThatThrownBy(() -> GraduationVideoKey.of("videos/graduation/43/550e8400.mp4", 42L))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
	}

	@Test
	void 다른_용도의_영상_키는_거절한다() {
		assertThatThrownBy(() -> GraduationVideoKey.of("videos/training/42/550e8400.mp4", 42L))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
	}

	@Test
	void 형식에_어긋난_키는_거절한다() {
		assertThatThrownBy(() -> GraduationVideoKey.of("videos/graduation/42/no-extension", 42L))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
		assertThatThrownBy(() -> GraduationVideoKey.of("videos/graduation/42/../43/x.mp4", 42L))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
		assertThatThrownBy(() -> GraduationVideoKey.of(null, 42L))
				.isInstanceOf(InvalidGraduationVideoKeyException.class);
	}
}
