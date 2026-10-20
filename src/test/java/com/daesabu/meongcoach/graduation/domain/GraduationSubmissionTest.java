package com.daesabu.meongcoach.graduation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GraduationSubmissionTest {

	private static final Instant SUBMITTED_AT = Instant.parse("2026-10-15T05:00:00Z");
	private static final Instant REPLACED_AT = Instant.parse("2026-10-16T05:00:00Z");
	private static final String VIDEO_KEY = "videos/graduation/42/7/clip.mp4";
	private static final String REPLACED_KEY = "videos/graduation/42/7/replaced.mp4";

	@Test
	void 수료_행의_현재_제출을_그대로_기록한다() {
		TopicGraduation graduation = TopicGraduation.start(42L, 7L, VIDEO_KEY, SUBMITTED_AT);
		ReflectionTestUtils.setField(graduation, "id", 1L);

		GraduationSubmission submission = GraduationSubmission.record(graduation);

		assertThat(submission.getGraduationId()).isEqualTo(1L);
		assertThat(submission.getAttemptNo()).isEqualTo(1);
		assertThat(submission.getVideoObjectKey()).isEqualTo(VIDEO_KEY);
		assertThat(submission.getSubmittedAt()).isEqualTo(SUBMITTED_AT);
	}

	@Test
	void 같은_차수의_기록을_수료_행의_새_영상으로_바꾼다() {
		TopicGraduation graduation = TopicGraduation.start(42L, 7L, VIDEO_KEY, SUBMITTED_AT);
		ReflectionTestUtils.setField(graduation, "id", 1L);
		GraduationSubmission submission = GraduationSubmission.record(graduation);
		graduation.resubmit(REPLACED_KEY, REPLACED_AT);

		submission.replaceVideo(graduation);

		assertThat(submission.getAttemptNo()).isEqualTo(1);
		assertThat(submission.getVideoObjectKey()).isEqualTo(REPLACED_KEY);
		assertThat(submission.getSubmittedAt()).isEqualTo(REPLACED_AT);
	}

	@Test
	void 차수가_다른_수료_행으로는_기록을_바꿀_수_없다() {
		TopicGraduation graduation = TopicGraduation.start(42L, 7L, VIDEO_KEY, SUBMITTED_AT);
		ReflectionTestUtils.setField(graduation, "id", 1L);
		GraduationSubmission submission = GraduationSubmission.record(graduation);
		graduation.useSubmission(submission);
		graduation.requestRevision("다시 찍어 주세요.", REPLACED_AT);
		graduation.resubmit(REPLACED_KEY, REPLACED_AT);

		assertThatThrownBy(() -> submission.replaceVideo(graduation)).isInstanceOf(IllegalStateException.class);
	}
	@Test
	void 판정한_제출의_영상은_교체할_수_없다() {
		TopicGraduation graduation = TopicGraduationFixture.create(1L, 42L, 7L,
				GraduationStatus.SUBMITTED, 1, VIDEO_KEY, SUBMITTED_AT, null, null);
		GraduationSubmission submission = graduation.getCurrentSubmission();
		graduation.approve("잘했어요.", REPLACED_AT);

		assertThatThrownBy(() -> submission.replaceVideo(graduation)).isInstanceOf(IllegalStateException.class);
		assertThat(submission.getVideoObjectKey()).isEqualTo(VIDEO_KEY);
		assertThat(submission.getReviewNote()).isEqualTo("잘했어요.");
	}

	@Test
	void 다른_수료의_같은_차수로_영상을_교체할_수_없다() {
		TopicGraduation first = TopicGraduationFixture.create(1L, 42L, 7L,
				GraduationStatus.SUBMITTED, 1, VIDEO_KEY, SUBMITTED_AT, null, null);
		TopicGraduation other = TopicGraduationFixture.create(2L, 42L, 8L,
				GraduationStatus.SUBMITTED, 1, REPLACED_KEY, REPLACED_AT, null, null);

		assertThatThrownBy(() -> first.getCurrentSubmission().replaceVideo(other)).isInstanceOf(IllegalStateException.class);
	}

}
