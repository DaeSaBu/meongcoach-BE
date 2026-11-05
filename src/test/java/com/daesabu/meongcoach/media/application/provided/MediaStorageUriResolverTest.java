package com.daesabu.meongcoach.media.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidMediaObjectKeyException;
import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class MediaStorageUriResolverTest {

	private static final MediaScope TEST_VIDEO = MediaScope.of(MediaKind.VIDEO, "test-analysis");

	@Autowired
	private MediaStorageUriResolver resolver;

	@Test
	void 범위_아래의_객체_키면_GCS_gs_URI를_반환한다() {
		String uri = resolver.resolve(TEST_VIDEO, "videos/test-analysis/7/a.mp4");

		assertThat(uri).isEqualTo("gs://test-ai-video-bucket/videos/test-analysis/7/a.mp4");
	}

	@Test
	void 범위_아래의_객체_키가_아니면_조회에_실패한다() {
		assertThatThrownBy(() -> resolver.resolve(TEST_VIDEO, "videos/training/7/a.mp4"))
				.isInstanceOf(InvalidMediaObjectKeyException.class);
	}
}
