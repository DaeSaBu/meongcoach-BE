package com.daesabu.meongcoach.graduation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.domain.exception.ReviewNoteRequiredException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GraduationSubmissionTest {

	private static final Instant SUBMITTED_AT = Instant.parse("2026-10-15T05:00:00Z");
	private static final Instant REVIEWED_AT = Instant.parse("2026-10-16T05:00:00Z");
	private static final String VIDEO_KEY = "videos/graduation/42/clip.mp4";

	@Test
	void 새_제출은_검수_대기_결과로_만든다() {
		GraduationSubmission submission = pending(2);

		assertThat(submission.getGraduationId()).isEqualTo(1L);
		assertThat(submission.getAttemptNo()).isEqualTo(2);
		assertThat(submission.getVideoObjectKey()).isEqualTo(VIDEO_KEY);
		assertThat(submission.getSubmittedAt()).isEqualTo(SUBMITTED_AT);
		assertThat(submission.isPending()).isTrue();
	}

	@Test
	void 대기_중인_제출을_교체하면_교체_결과로_남는다() {
		GraduationSubmission submission = pending(1);

		submission.replace();

		assertThat(submission.getResult()).isEqualTo(SubmissionResult.REPLACED);
	}

	@Test
	void 승인하면_판정_시각과_다듬은_메모를_기록한다() {
		GraduationSubmission submission = pending(1);

		submission.approve("  잘했어요  ", REVIEWED_AT);

		assertThat(submission.getResult()).isEqualTo(SubmissionResult.APPROVED);
		assertThat(submission.getReviewedAt()).isEqualTo(REVIEWED_AT);
		assertThat(submission.getReviewerNote()).isEqualTo("잘했어요");
	}

	@Test
	void 빈_메모로_승인하면_메모_없이_승인한다() {
		GraduationSubmission submission = pending(1);

		submission.approve(" ", REVIEWED_AT);

		assertThat(submission.getReviewerNote()).isNull();
	}

	@Test
	void 보완_요청은_메모와_함께_기록한다() {
		GraduationSubmission submission = pending(1);

		submission.requestRevision("현관 장면부터 다시 찍어 주세요.", REVIEWED_AT);

		assertThat(submission.getResult()).isEqualTo(SubmissionResult.REVISION_REQUESTED);
		assertThat(submission.getReviewerNote()).isEqualTo("현관 장면부터 다시 찍어 주세요.");
	}

	@Test
	void 보완_요청에_메모가_없으면_예외를_던진다() {
		GraduationSubmission submission = pending(1);

		assertThatThrownBy(() -> submission.requestRevision("  ", REVIEWED_AT))
				.isInstanceOf(ReviewNoteRequiredException.class);
		assertThatThrownBy(() -> submission.requestRevision(null, REVIEWED_AT))
				.isInstanceOf(ReviewNoteRequiredException.class);
	}

	@Test
	void 대기_중이_아닌_제출은_판정하거나_교체할_수_없다() {
		GraduationSubmission submission = pending(1);
		submission.approve(null, REVIEWED_AT);

		assertThatThrownBy(() -> submission.approve(null, REVIEWED_AT)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> submission.requestRevision("메모", REVIEWED_AT)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(submission::replace).isInstanceOf(IllegalStateException.class);
	}

	private GraduationSubmission pending(int attemptNo) {
		TopicGraduation graduation = TopicGraduation.start(42L, 7L);
		ReflectionTestUtils.setField(graduation, "id", 1L);
		return GraduationSubmission.create(graduation,
				new GraduationSubmissionCreateCommand(attemptNo, VIDEO_KEY, SUBMITTED_AT));
	}
}
