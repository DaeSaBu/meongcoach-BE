package com.daesabu.meongcoach.graduation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TopicGraduationTest {

	private static final Instant NOW = Instant.parse("2026-10-15T05:00:00Z");
	private static final Instant LATER = Instant.parse("2026-10-16T05:00:00Z");
	private static final String FIRST_KEY = "videos/graduation/42/7/first.mp4";
	private static final String SECOND_KEY = "videos/graduation/42/7/second.mp4";

	@Test
	void 처음_시작한_수료는_1번째_제출로_제출_상태다() {
		TopicGraduation graduation = started();

		assertThat(graduation.getUserId()).isEqualTo(42L);
		assertThat(graduation.getTopicId()).isEqualTo(7L);
		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(graduation.getAttemptNo()).isEqualTo(1);
		assertThat(graduation.getVideoObjectKey()).isEqualTo(FIRST_KEY);
		assertThat(graduation.getSubmittedAt()).isEqualTo(NOW);
	}

	@Test
	void 검수_대기_중에_다시_제출하면_차수를_유지하고_영상만_바꾼다() {
		TopicGraduation graduation = started();

		graduation.resubmit(SECOND_KEY, LATER);

		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(graduation.getAttemptNo()).isEqualTo(1);
		assertThat(graduation.getVideoObjectKey()).isEqualTo(SECOND_KEY);
		assertThat(graduation.getSubmittedAt()).isEqualTo(LATER);
	}

	@Test
	void 보완_요청_뒤에_다시_제출하면_차수를_올리고_이전_제출의_판정을_보존한다() {
		TopicGraduation graduation = started();
		GraduationSubmission submission = GraduationSubmission.from(graduation);
		graduation.requestRevision(submission, "현관 장면부터 다시 찍어 주세요.", LATER);
		TopicGraduationFixture.withRetakeGuide(submission, "https://images.test/images/graduation-retake/3/example.jpg", List.of("현관이 함께 보이게 찍어 주세요."));

		graduation.resubmit(SECOND_KEY, LATER);

		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(graduation.getAttemptNo()).isEqualTo(2);
		assertThat(graduation.getVideoObjectKey()).isEqualTo(SECOND_KEY);
		assertThat(graduation.getSubmittedAt()).isEqualTo(LATER);
		assertThat(submission.getReviewedAt()).isEqualTo(LATER);
		assertThat(submission.getReviewNote()).isEqualTo("현관 장면부터 다시 찍어 주세요.");
		assertThat(submission.getRetakeImageUrl()).isEqualTo("https://images.test/images/graduation-retake/3/example.jpg");
		assertThat(submission.getRetakeInstructions()).containsExactly("현관이 함께 보이게 찍어 주세요.");
	}

	@Test
	void 승인된_수료에는_다시_제출할_수_없다() {
		TopicGraduation graduation = approved();

		assertThatThrownBy(() -> graduation.resubmit(SECOND_KEY, LATER)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 승인하면_승인_시각과_다듬은_메모를_기록한다() {
		TopicGraduation graduation = started();
		GraduationSubmission submission = GraduationSubmission.from(graduation);

		graduation.approve(submission, "  잘했어요  ", LATER);

		assertThat(graduation.isApproved()).isTrue();
		assertThat(submission.getReviewedAt()).isEqualTo(LATER);
		assertThat(graduation.getApprovedAt()).isEqualTo(LATER);
		assertThat(submission.getReviewNote()).isEqualTo("잘했어요");
	}

	@Test
	void 빈_메모로_승인하면_메모_없이_승인한다() {
		TopicGraduation graduation = started();
		GraduationSubmission submission = GraduationSubmission.from(graduation);

		graduation.approve(submission, " ", LATER);

		assertThat(submission.getReviewNote()).isNull();
	}

	@Test
	void 보완_요청은_메모와_판정_시각을_기록한다() {
		TopicGraduation graduation = started();
		GraduationSubmission submission = GraduationSubmission.from(graduation);

		graduation.requestRevision(submission, "현관 장면부터 다시 찍어 주세요.", LATER);

		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.REVISION_REQUESTED);
		assertThat(submission.getReviewedAt()).isEqualTo(LATER);
		assertThat(submission.getReviewNote()).isEqualTo("현관 장면부터 다시 찍어 주세요.");
	}

	@Test
	void 보완_요청에_메모가_없으면_예외를_던진다() {
		TopicGraduation graduation = started();
		GraduationSubmission submission = GraduationSubmission.from(graduation);

		assertThatThrownBy(() -> graduation.requestRevision(submission, "  ", LATER)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> graduation.requestRevision(submission, null, LATER)).isInstanceOf(IllegalArgumentException.class);
		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
	}

	@Test
	void 제출_상태가_아니면_판정할_수_없다() {
		TopicGraduation graduation = approved();
		GraduationSubmission submission = TopicGraduationFixture.submission(graduation, LATER, null);

		assertThatThrownBy(() -> graduation.approve(submission, null, LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> graduation.requestRevision(submission, "메모", LATER)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 다른_수료의_제출로_승인하거나_보완_요청할_수_없다() {
		TopicGraduation graduation = started();
		TopicGraduation other = TopicGraduationFixture.create(2L, 42L, 8L,
				GraduationStatus.SUBMITTED, 1, FIRST_KEY, NOW, null, null);
		GraduationSubmission submission = GraduationSubmission.from(other);

		assertThatThrownBy(() -> graduation.approve(submission, "승인", LATER))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> graduation.requestRevision(submission, "보완", LATER))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(submission.getStatus()).isEqualTo(GraduationSubmissionStatus.SUBMITTED);
	}

	@Test
	void 이전_차수의_제출로_현재_수료를_판정할_수_없다() {
		TopicGraduation graduation = started();
		GraduationSubmission previous = GraduationSubmission.from(graduation);
		graduation.requestRevision(previous, "다시 촬영", LATER);
		graduation.resubmit(SECOND_KEY, LATER);

		assertThatThrownBy(() -> graduation.approve(previous, "승인", LATER))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> graduation.requestRevision(previous, "보완", LATER))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(previous.getStatus()).isEqualTo(GraduationSubmissionStatus.REVISION_REQUESTED);
	}

	private TopicGraduation started() {
		return TopicGraduationFixture.create(1L, 42L, 7L, GraduationStatus.SUBMITTED, 1, FIRST_KEY, NOW, null, null);
	}

	private TopicGraduation approved() {
		TopicGraduation graduation = started();
		GraduationSubmission submission = GraduationSubmission.from(graduation);
		graduation.approve(submission, null, LATER);
		return graduation;
	}
}
