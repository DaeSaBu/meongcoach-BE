package com.daesabu.meongcoach.media.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.VideoType;
import com.daesabu.meongcoach.media.domain.VideoUploadTarget;
import com.daesabu.meongcoach.media.domain.exception.InvalidObjectKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("영상 객체 키 값 객체")
class VideoObjectKeyTest {

	private static final String VALID_KEY = "videos/ai-analysis/42/550e8400-e29b-41d4-a716-446655440000.mp4";

	@Test
	@DisplayName("AI 분석 영상의 객체 키는 대상·사용자 ID·확장자를 담는다")
	void createBuildsAiAnalysisKey() {
		VideoObjectKey key = VideoObjectKey.create(VideoUploadTarget.AI_ANALYSIS, 7L, VideoType.MP4);

		assertThat(key.value()).matches("videos/ai-analysis/7/[0-9a-f-]{36}\\.mp4");
	}

	@Test
	@DisplayName("퀵타임 영상의 객체 키는 mov 확장자를 쓴다")
	void createBuildsQuicktimeKey() {
		VideoObjectKey key = VideoObjectKey.create(VideoUploadTarget.AI_ANALYSIS, 7L, VideoType.QUICKTIME);

		assertThat(key.value()).matches("videos/ai-analysis/7/[0-9a-f-]{36}\\.mov");
	}

	@Test
	@DisplayName("생성할 때마다 서로 다른 객체 키를 만든다")
	void createGeneratesUniqueKeys() {
		VideoObjectKey first = VideoObjectKey.create(VideoUploadTarget.AI_ANALYSIS, 7L, VideoType.MP4);
		VideoObjectKey second = VideoObjectKey.create(VideoUploadTarget.AI_ANALYSIS, 7L, VideoType.MP4);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	@DisplayName("우리가 만든 형식의 문자열은 객체 키로 되돌린다")
	void parseAcceptsValidKey() {
		VideoObjectKey key = VideoObjectKey.parse(VALID_KEY);

		assertThat(key.value()).isEqualTo(VALID_KEY);
	}

	@Test
	@DisplayName("생성한 객체 키는 다시 파싱할 수 있다")
	void parseAcceptsCreatedKey() {
		VideoObjectKey created = VideoObjectKey.create(VideoUploadTarget.AI_ANALYSIS, 7L, VideoType.MP4);

		assertThat(VideoObjectKey.parse(created.value())).isEqualTo(created);
	}

	@Test
	@DisplayName("null은 거부한다")
	void parseRejectsNull() {
		assertThatThrownBy(() -> VideoObjectKey.parse(null))
				.isInstanceOf(InvalidObjectKeyException.class);
	}

	@Test
	@DisplayName("공백 문자열은 거부한다")
	void parseRejectsBlank() {
		assertThatThrownBy(() -> VideoObjectKey.parse("   "))
				.isInstanceOf(InvalidObjectKeyException.class);
	}

	@Test
	@DisplayName("조각이 4개가 아니면 거부한다")
	void parseRejectsWrongSegmentCount() {
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/ai-analysis/42"))
				.isInstanceOf(InvalidObjectKeyException.class);
	}

	@Test
	@DisplayName("첫 조각이 videos가 아니면 거부한다")
	void parseRejectsWrongPrefix() {
		assertThatThrownBy(() -> VideoObjectKey.parse("images/ai-analysis/42/key.mp4"))
				.isInstanceOf(InvalidObjectKeyException.class);
	}

	@Test
	@DisplayName("정의되지 않은 업로드 대상 경로는 거부한다")
	void parseRejectsUndefinedTarget() {
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/banner/42/key.mp4"))
				.isInstanceOf(InvalidObjectKeyException.class);
	}

	@Test
	@DisplayName("사용자 ID 자리가 숫자가 아니면 거부한다")
	void parseRejectsNonNumericOwnerId() {
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/ai-analysis/me/key.mp4"))
				.isInstanceOf(InvalidObjectKeyException.class);
	}

	@Test
	@DisplayName("사용자 ID 자리가 양수가 아니면 거부한다")
	void parseRejectsNonPositiveOwnerId() {
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/ai-analysis/0/key.mp4"))
				.isInstanceOf(InvalidObjectKeyException.class);
	}

	@Test
	@DisplayName("허용 목록에 없는 확장자는 거부한다")
	void parseRejectsUnsupportedExtension() {
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/ai-analysis/42/key.avi"))
				.isInstanceOf(InvalidObjectKeyException.class);
	}

	@Test
	@DisplayName("확장자가 없으면 거부한다")
	void parseRejectsMissingExtension() {
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/ai-analysis/42/key"))
				.isInstanceOf(InvalidObjectKeyException.class);
	}

	@Test
	@DisplayName("상위 경로 표기가 섞이면 거부한다")
	void parseRejectsParentPath() {
		assertThatThrownBy(() -> VideoObjectKey.parse("videos/ai-analysis/../1/a.mp4"))
				.isInstanceOf(InvalidObjectKeyException.class);
	}

	@Test
	@DisplayName("슬래시로 시작하면 거부한다")
	void parseRejectsLeadingSeparator() {
		assertThatThrownBy(() -> VideoObjectKey.parse("/videos/ai-analysis/1/a.mp4"))
				.isInstanceOf(InvalidObjectKeyException.class);
	}

	@Test
	@DisplayName("키에서 소유자의 사용자 ID를 꺼낸다")
	void ownerIdReadsUserIdFromKey() {
		assertThat(VideoObjectKey.parse(VALID_KEY).ownerId()).isEqualTo(42L);
	}

	@Test
	@DisplayName("소유자가 같으면 참이다")
	void belongsToIsTrueForOwner() {
		assertThat(VideoObjectKey.parse(VALID_KEY).belongsTo(42L)).isTrue();
	}

	@Test
	@DisplayName("소유자가 다르면 거짓이다")
	void belongsToIsFalseForOtherUser() {
		assertThat(VideoObjectKey.parse(VALID_KEY).belongsTo(43L)).isFalse();
	}

	@Test
	@DisplayName("사용자를 알 수 없으면 거짓이다")
	void belongsToIsFalseForNullUser() {
		assertThat(VideoObjectKey.parse(VALID_KEY).belongsTo(null)).isFalse();
	}

	@Test
	@DisplayName("같은 값끼리는 동등하다")
	void sameValuesAreEqual() {
		assertThat(VideoObjectKey.parse(VALID_KEY)).isEqualTo(VideoObjectKey.parse(VALID_KEY));
	}
}
