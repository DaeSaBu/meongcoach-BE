package com.daesabu.meongcoach.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidUploadTargetException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이미지 업로드 대상")
class ImageUploadTargetTest {

	@Test
	@DisplayName("문자열 코드를 enum으로 변환한다")
	void fromConvertsCodeToEnum() {
		assertThat(ImageUploadTarget.from("USER_PROFILE")).isEqualTo(ImageUploadTarget.USER_PROFILE);
		assertThat(ImageUploadTarget.from("DOG_PROFILE")).isEqualTo(ImageUploadTarget.DOG_PROFILE);
	}

	@Test
	@DisplayName("대상마다 객체 키 경로 구획을 가진다")
	void targetHasPathSegment() {
		assertThat(ImageUploadTarget.USER_PROFILE.getPathSegment()).isEqualTo("user-profile");
		assertThat(ImageUploadTarget.DOG_PROFILE.getPathSegment()).isEqualTo("dog-profile");
	}

	@Test
	@DisplayName("지원하지 않는 값이면 변환에 실패한다")
	void fromFailsWhenValueIsUnknown() {
		assertThatThrownBy(() -> ImageUploadTarget.from("BANNER"))
				.isInstanceOf(InvalidUploadTargetException.class);
	}

	@Test
	@DisplayName("값이 없으면 변환에 실패한다")
	void fromFailsWhenValueIsNull() {
		assertThatThrownBy(() -> ImageUploadTarget.from(null))
				.isInstanceOf(InvalidUploadTargetException.class);
	}
}
