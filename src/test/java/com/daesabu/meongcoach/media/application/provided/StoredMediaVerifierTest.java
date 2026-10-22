package com.daesabu.meongcoach.media.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidMediaUrlException;
import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class StoredMediaVerifierTest {

	private static final String PUBLIC_BASE_URL = "https://images.test.meongcoach.com/";
	private static final MediaScope DOG_IMAGE = MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC, "dog-profile");
	private static final MediaScope GRADUATION_VIDEO =
			MediaScope.of(MediaKind.VIDEO, MediaVisibility.PRIVATE, "graduation");

	@Autowired
	private StoredMediaVerifier verifier;

	@Test
	void 범위와_사용자에_맞는_우리_스토리지의_공개_URL이면_통과한다() {
		assertThatCode(() -> verifier.validateUrl(DOG_IMAGE, 7L, PUBLIC_BASE_URL + "images/dog-profile/7/a.jpg"))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = " ")
	void URL이_없으면_통과한다(String url) {
		assertThatCode(() -> verifier.validateUrl(DOG_IMAGE, 7L, url)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"https://evil.example.com/images/dog-profile/7/a.jpg",
			"https://images.test.meongcoach.com.evil.example.com/images/dog-profile/7/a.jpg",
			PUBLIC_BASE_URL + "images/dog-profile/8/a.jpg",
			PUBLIC_BASE_URL + "images/user-profile/7/a.jpg",
			PUBLIC_BASE_URL + "videos/dog-profile/7/a.mp4",
			PUBLIC_BASE_URL + "images/dog-profile/7/sub/a.jpg"})
	void 범위나_사용자가_다르거나_우리_스토리지가_아닌_URL이면_실패한다(String url) {
		assertThatThrownBy(() -> verifier.validateUrl(DOG_IMAGE, 7L, url))
				.isInstanceOf(InvalidMediaUrlException.class);
	}

	@Test
	void 범위와_사용자와_하위_경로가_맞는_객체_키면_true를_반환한다() {
		boolean owned = verifier.isOwnedObjectKey(GRADUATION_VIDEO.withSubPath("1101"), 7L,
				"videos/graduation/7/1101/a.mp4");

		assertThat(owned).isTrue();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"videos/graduation/8/1101/a.mp4", "videos/graduation/7/1102/a.mp4",
			"videos/training/7/a.mp4", "videos/graduation/7/1101/a"})
	void 범위나_사용자나_하위_경로가_다른_객체_키면_false를_반환한다(String objectKey) {
		boolean owned = verifier.isOwnedObjectKey(GRADUATION_VIDEO.withSubPath("1101"), 7L, objectKey);

		assertThat(owned).isFalse();
	}
}
