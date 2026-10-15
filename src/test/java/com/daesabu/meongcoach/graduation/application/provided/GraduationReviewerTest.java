package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.GraduationSubmissionRepository;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.SubmissionResult;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.InvalidReviewDecisionException;
import com.daesabu.meongcoach.graduation.domain.exception.ReviewNoteRequiredException;
import com.daesabu.meongcoach.graduation.domain.exception.SubmissionNotFoundException;
import com.daesabu.meongcoach.graduation.domain.exception.SubmissionNotPendingException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@ApplicationTest
@Import(TopicSeeder.class)
class GraduationReviewerTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private GraduationReviewer graduationReviewer;

	@Autowired
	private GraduationSubmitter graduationSubmitter;

	@Autowired
	private TopicGraduationRepository topicGraduationRepository;

	@Autowired
	private GraduationSubmissionRepository graduationSubmissionRepository;

	@Autowired
	private TopicSeeder topicSeeder;

	private Topic topic;

	@BeforeEach
	void setUp() {
		topic = topicSeeder.completedTopic(USER_ID, "분리불안");
	}

	@Test
	void 검수_대기_제출을_승인하면_제출과_수료가_함께_승인된다() {
		GraduationSubmission pending = submit("first");

		GraduationSubmission reviewed = graduationReviewer.review(pending.getId(),
				new GraduationReviewRequest("APPROVED", "끝까지 함께 시도했어요!"));
		topicSeeder.flushAndClear();

		assertThat(reviewed.getResult()).isEqualTo(SubmissionResult.APPROVED);
		assertThat(reviewed.getReviewerNote()).isEqualTo("끝까지 함께 시도했어요!");
		assertThat(reviewed.getReviewedAt()).isNotNull();
		TopicGraduation graduation = topicGraduationRepository.findById(pending.getGraduationId()).orElseThrow();
		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.APPROVED);
		assertThat(graduation.getApprovedAt()).isNotNull();
	}

	@Test
	void 보완을_요청하면_제출과_수료가_함께_보완_요청_상태가_된다() {
		GraduationSubmission pending = submit("first");

		graduationReviewer.review(pending.getId(), new GraduationReviewRequest("REVISION_REQUESTED", "현관 장면부터 다시 찍어 주세요."));
		topicSeeder.flushAndClear();

		assertThat(graduationSubmissionRepository.findById(pending.getId()).orElseThrow().getResult())
				.isEqualTo(SubmissionResult.REVISION_REQUESTED);
		assertThat(topicGraduationRepository.findById(pending.getGraduationId()).orElseThrow().getStatus())
				.isEqualTo(GraduationStatus.REVISION_REQUESTED);
	}

	@Test
	void 보완_요청에_메모가_없으면_예외를_던진다() {
		GraduationSubmission pending = submit("first");

		assertThatThrownBy(() -> graduationReviewer.review(pending.getId(), new GraduationReviewRequest("REVISION_REQUESTED", " ")))
				.isInstanceOf(ReviewNoteRequiredException.class);
	}

	@Test
	void 이미_판정한_제출을_다시_판정하면_예외를_던진다() {
		GraduationSubmission pending = submit("first");
		graduationReviewer.review(pending.getId(), new GraduationReviewRequest("APPROVED", null));
		topicSeeder.flushAndClear();

		assertThatThrownBy(() -> graduationReviewer.review(pending.getId(), new GraduationReviewRequest("APPROVED", null)))
				.isInstanceOf(SubmissionNotPendingException.class);
	}

	@Test
	void 교체된_제출을_판정하면_예외를_던진다() {
		GraduationSubmission first = submit("first");
		submit("second");

		assertThatThrownBy(() -> graduationReviewer.review(first.getId(), new GraduationReviewRequest("APPROVED", null)))
				.isInstanceOf(SubmissionNotPendingException.class);
	}

	@Test
	void 존재하지_않는_제출을_판정하면_예외를_던진다() {
		assertThatThrownBy(() -> graduationReviewer.review(999L, new GraduationReviewRequest("APPROVED", null)))
				.isInstanceOf(SubmissionNotFoundException.class);
	}

	@Test
	void 지원하지_않는_판정이면_예외를_던진다() {
		GraduationSubmission pending = submit("first");

		assertThatThrownBy(() -> graduationReviewer.review(pending.getId(), new GraduationReviewRequest("REJECTED", "메모")))
				.isInstanceOf(InvalidReviewDecisionException.class);
	}

	private GraduationSubmission submit(String name) {
		GraduationSubmission submission = graduationSubmitter.submit(USER_ID, topic.getId(),
				new GraduationSubmissionRequest("videos/graduation/42/" + name + ".mp4"));
		topicSeeder.flushAndClear();
		return submission;
	}
}
