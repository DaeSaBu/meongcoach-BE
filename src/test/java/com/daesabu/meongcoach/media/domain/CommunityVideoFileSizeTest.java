package com.daesabu.meongcoach.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommunityVideoFileSizeTest {

	@Test
	void 상한과_정확히_같은_크기는_허용한다() {
		assertThatCode(() -> new CommunityVideoFileSize(CommunityVideoFileSize.MAX_BYTES))
				.doesNotThrowAnyException();
	}

	@Test
	void 상한을_1바이트라도_넘으면_생성에_실패한다() {
		assertThatThrownBy(() -> new CommunityVideoFileSize(CommunityVideoFileSize.MAX_BYTES + 1))
				.isInstanceOf(VideoFileSizeExceededException.class);
	}

	@ParameterizedTest
	@ValueSource(longs = {0L, -1L})
	void 크기가_0_이하면_생성에_실패한다(long bytes) {
		assertThatThrownBy(() -> new CommunityVideoFileSize(bytes))
				.isInstanceOf(InvalidVideoFileSizeException.class);
	}

	@Test
	void 상한은_512MiB다() {
		assertThat(CommunityVideoFileSize.MAX_BYTES).isEqualTo(536_870_912L);
	}
}
