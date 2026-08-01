package com.daesabu.meongcoach.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidUploadTargetException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("영상 업로드 대상")
class VideoUploadTargetTest {

	@Test
	@DisplayName("문자열 코드를 enum으로 변환한다")
	void fromConvertsCodeToEnum() {
		assertThat(VideoUploadTarget.from("AI_ANALYSIS")).isEqualTo(VideoUploadTarget.AI_ANALYSIS);
	}

	@Test
	@DisplayName("대상마다 객체 키 경로 구획을 가진다")
	void targetHasPathSegment() {
		assertThat(VideoUploadTarget.AI_ANALYSIS.getPathSegment()).isEqualTo("ai-analysis");
	}

	@Test
	@DisplayName("정의되지 않은 값이면 변환에 실패한다")
	void fromFailsWhenValueIsUnknown() {
		assertThatThrownBy(() -> VideoUploadTarget.from("BANNER"))
				.isInstanceOf(InvalidUploadTargetException.class);
	}

	@Test
	@DisplayName("값이 없으면 변환에 실패한다")
	void fromFailsWhenValueIsNull() {
		assertThatThrownBy(() -> VideoUploadTarget.from(null))
				.isInstanceOf(InvalidUploadTargetException.class);
	}

	@Test
	@DisplayName("경로 구획을 enum으로 변환한다")
	void fromPathSegmentConvertsToEnum() {
		assertThat(VideoUploadTarget.fromPathSegment("ai-analysis")).isEqualTo(VideoUploadTarget.AI_ANALYSIS);
	}

	@Test
	@DisplayName("정의되지 않은 경로 구획이면 변환에 실패한다")
	void fromPathSegmentFailsWhenSegmentIsUnknown() {
		assertThatThrownBy(() -> VideoUploadTarget.fromPathSegment("banner"))
				.isInstanceOf(InvalidUploadTargetException.class);
	}

	@Test
	@DisplayName("경로 구획이 없으면 변환에 실패한다")
	void fromPathSegmentFailsWhenSegmentIsNull() {
		assertThatThrownBy(() -> VideoUploadTarget.fromPathSegment(null))
				.isInstanceOf(InvalidUploadTargetException.class);
	}
}
