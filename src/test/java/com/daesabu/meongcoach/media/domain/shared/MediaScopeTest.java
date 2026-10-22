package com.daesabu.meongcoach.media.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MediaScopeTest {

	@Test
	void 사용자_경로는_매체_디렉터리_경로_범위_사용자_ID_순서다() {
		MediaScope scope = MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC, "community-post");

		assertThat(scope.prefixOf(7L)).isEqualTo("images/community-post/7/");
	}

	@Test
	void 하위_경로가_있으면_사용자_ID_뒤에_붙는다() {
		MediaScope scope = MediaScope.of(MediaKind.VIDEO, MediaVisibility.PRIVATE, "graduation").withSubPath("1101");

		assertThat(scope.prefixOf(7L)).isEqualTo("videos/graduation/7/1101/");
	}

	@Test
	void 루트_경로는_사용자_ID_앞까지다() {
		MediaScope scope = MediaScope.of(MediaKind.VIDEO, MediaVisibility.PRIVATE, "graduation").withSubPath("1101");

		assertThat(scope.rootPrefix()).isEqualTo("videos/graduation/");
	}

	@Test
	void 여러_구획으로_된_경로_범위를_허용한다() {
		MediaScope scope = MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC, "community/post-1");

		assertThat(scope.prefixOf(7L)).isEqualTo("images/community/post-1/7/");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"/community", "community/", "Community", "a/../b", "a//b", "a b", "a.b"})
	void 형식에_맞지_않는_경로_범위는_만들지_않는다(String path) {
		assertThatThrownBy(() -> MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC, path))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "/1101", "..", "a b"})
	void 형식에_맞지_않는_하위_경로는_만들지_않는다(String subPath) {
		MediaScope scope = MediaScope.of(MediaKind.VIDEO, MediaVisibility.PRIVATE, "graduation");

		assertThatThrownBy(() -> scope.withSubPath(subPath))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 사용자_ID_없이는_사용자_경로를_만들지_않는다() {
		MediaScope scope = MediaScope.of(MediaKind.IMAGE, MediaVisibility.PUBLIC, "community-post");

		assertThatThrownBy(() -> scope.prefixOf(null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
