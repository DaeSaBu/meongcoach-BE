package com.daesabu.meongcoach.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoUploadTargetException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("영상 업로드 대상")
class VideoUploadTargetTest {

	@Test
	@DisplayName("문자열 코드를 enum으로 변환한다")
	void fromConvertsCodeToEnum() {
		assertThat(VideoUploadTarget.from("TRAINING_VIDEO")).isEqualTo(VideoUploadTarget.TRAINING_VIDEO);
	}

	@Test
	@DisplayName("대상마다 객체 키 경로 구획을 가진다")
	void targetHasPathSegment() {
		assertThat(VideoUploadTarget.TRAINING_VIDEO.getPathSegment()).isEqualTo("training");
	}

	@Test
	@DisplayName("이미지 업로드 대상은 영상 대상으로 변환되지 않는다")
	void fromFailsWhenValueIsImageTarget() {
		assertThatThrownBy(() -> VideoUploadTarget.from("USER_PROFILE"))
				.isInstanceOf(InvalidVideoUploadTargetException.class);
	}

	@Test
	@DisplayName("알 수 없는 대상이면 변환에 실패한다")
	void fromFailsWhenValueIsUnknown() {
		assertThatThrownBy(() -> VideoUploadTarget.from("BANNER"))
				.isInstanceOf(InvalidVideoUploadTargetException.class);
	}

	@Test
	@DisplayName("대상이 없으면 변환에 실패한다")
	void fromFailsWhenValueIsNull() {
		assertThatThrownBy(() -> VideoUploadTarget.from(null))
				.isInstanceOf(InvalidVideoUploadTargetException.class);
	}
}
