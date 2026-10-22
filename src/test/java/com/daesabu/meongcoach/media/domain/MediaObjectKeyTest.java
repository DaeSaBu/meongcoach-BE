package com.daesabu.meongcoach.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidMediaObjectKeyException;
import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class MediaObjectKeyTest {

	private static final MediaScope COMMUNITY_IMAGE =
			MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC, "community-post");
	private static final MediaScope TRAINING_VIDEO =
			MediaScope.of(MediaKind.VIDEO, MediaVisibility.PRIVATE, "training");
	private static final MediaScope GRADUATION_VIDEO =
			MediaScope.of(MediaKind.VIDEO, MediaVisibility.PRIVATE, "graduation");

	@Test
	void 객체_키는_사용자_경로_아래_UUID_파일명과_확장자를_담는다() {
		MediaObjectKey key = MediaObjectKey.create(COMMUNITY_IMAGE, 7L, "png");

		assertThat(key.value()).matches("images/community-post/7/[0-9a-f-]{36}\\.png");
	}

	@Test
	void 하위_경로가_있는_범위의_객체_키는_사용자_ID_뒤에_하위_경로를_담는다() {
		MediaObjectKey key = MediaObjectKey.create(GRADUATION_VIDEO.withSubPath("1101"), 7L, "mp4");

		assertThat(key.value()).matches("videos/graduation/7/1101/[0-9a-f-]{36}\\.mp4");
	}

	@Test
	void 같은_범위와_사용자로_만든_키는_그_범위와_사용자의_것이다() {
		MediaObjectKey key = MediaObjectKey.create(COMMUNITY_IMAGE, 7L, "png");

		assertThat(key.belongsTo(COMMUNITY_IMAGE, 7L)).isTrue();
	}

	@Test
	void 다른_사용자의_키는_그_사용자의_것이_아니다() {
		MediaObjectKey key = MediaObjectKey.create(COMMUNITY_IMAGE, 70L, "png");

		assertThat(key.belongsTo(COMMUNITY_IMAGE, 7L)).isFalse();
	}

	@Test
	void 다른_범위의_키는_그_범위의_것이_아니다() {
		MediaObjectKey key = MediaObjectKey.create(MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC,
				"dog-profile"), 7L, "png");

		assertThat(key.belongsTo(COMMUNITY_IMAGE, 7L)).isFalse();
	}

	@Test
	void 하위_경로가_다른_키는_그_하위_경로의_것이_아니다() {
		MediaObjectKey key = MediaObjectKey.create(GRADUATION_VIDEO.withSubPath("1101"), 7L, "mp4");

		assertThat(key.belongsTo(GRADUATION_VIDEO.withSubPath("1102"), 7L)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"images/community-post/7/a/b.png", "images/community-post/7/a", "images/community-post/7/"})
	void 사용자_경로_바로_아래의_파일이_아니면_그_사용자의_것이_아니다(String value) {
		assertThat(new MediaObjectKey(value).belongsTo(COMMUNITY_IMAGE, 7L)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {"videos/training/7/a.mp4", "videos/training/7/sub/a.mov"})
	void 범위_아래의_키는_해석한다(String value) {
		MediaObjectKey key = MediaObjectKey.parse(TRAINING_VIDEO, value);

		assertThat(key.value()).isEqualTo(value);
	}

	@Test
	void 하위_경로가_있는_키도_기본_범위로_해석한다() {
		MediaObjectKey key = MediaObjectKey.parse(GRADUATION_VIDEO, "videos/graduation/7/1101/a.mp4");

		assertThat(key.value()).isEqualTo("videos/graduation/7/1101/a.mp4");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"videos/graduation/7/a.mp4", "images/training/7/a.mp4", "videos/training/x/a.mp4",
			"videos/training/7/a", "videos/training/7/../a.mp4", "prefix/videos/training/7/a.mp4"})
	void 범위_아래의_키가_아니면_해석에_실패한다(String value) {
		assertThatThrownBy(() -> MediaObjectKey.parse(TRAINING_VIDEO, value))
				.isInstanceOf(InvalidMediaObjectKeyException.class);
	}
}
