package com.daesabu.meongcoach.graduation.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.daesabu.meongcoach.entitlement.application.required.PromotionalEntitlementGranter;
import com.daesabu.meongcoach.entitlement.domain.exception.EntitlementProviderUnavailableException;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationReviewRequest;
import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.application.required.TopicGraduationRepository;
import com.daesabu.meongcoach.graduation.domain.GraduationStatus;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationNotFoundException;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationNotPendingException;
import com.daesabu.meongcoach.graduation.domain.exception.InvalidReviewDecisionException;
import com.daesabu.meongcoach.graduation.domain.exception.ReviewNoteRequiredException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@ApplicationTest
@Import(TopicSeeder.class)
class GraduationReviewerTest {

	private static final Long USER_ID = 42L;
	private static final Duration THREE_DAYS = Duration.ofDays(3);

	@Autowired
	private GraduationReviewer graduationReviewer;

	@Autowired
	private GraduationSubmitter graduationSubmitter;

	@Autowired
	private TopicGraduationRepository topicGraduationRepository;

	@Autowired
	private PromotionalEntitlementGranter promotionalEntitlementGranter;

	@Autowired
	private TopicSeeder topicSeeder;

	@Test
	void 승인하면_네_종류의_이용권을_3일짜리로_지급하고_승인_시각과_만료_시각과_메모를_기록한다() {
		TopicGraduation pending = submit("분리불안");
		Instant before = Instant.now();

		TopicGraduation reviewed = graduationReviewer.review(pending.getId(),
				new GraduationReviewRequest("APPROVED", "끝까지 함께 시도했어요!"));
		topicSeeder.flushAndClear();

		Instant after = Instant.now();
		TopicGraduation saved = topicGraduationRepository.findById(pending.getId()).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(GraduationStatus.APPROVED);
		assertThat(saved.getReviewNote()).isEqualTo("끝까지 함께 시도했어요!");
		assertThat(saved.getApprovedAt()).isBetween(before, after);
		assertThat(saved.getRewardExpiresAt()).isEqualTo(saved.getApprovedAt().plus(THREE_DAYS));
		assertThat(reviewed.getRewardExpiresAt()).isEqualTo(saved.getRewardExpiresAt());
		for (EntitlementType type : EntitlementType.values()) {
			then(promotionalEntitlementGranter).should().grant(USER_ID, type, saved.getRewardExpiresAt());
		}
	}

	@Test
	void 이전_보상_기간이_남아_있으면_그_뒤로_3일을_잇는다() {
		TopicGraduation first = submit("분리불안");
		TopicGraduation second = submit("산책");
		TopicGraduation firstApproved = graduationReviewer.review(first.getId(), new GraduationReviewRequest("APPROVED", null));
		topicSeeder.flushAndClear();

		TopicGraduation secondApproved = graduationReviewer.review(second.getId(), new GraduationReviewRequest("APPROVED", null));

		assertThat(secondApproved.getRewardExpiresAt()).isEqualTo(firstApproved.getRewardExpiresAt().plus(THREE_DAYS));
	}

	@Test
	void 지급_도중_실패하면_검수_대기로_남고_다시_승인하면_네_종류를_다시_지급한다() {
		TopicGraduation pending = submit("분리불안");
		willThrow(new EntitlementProviderUnavailableException())
				.given(promotionalEntitlementGranter).grant(eq(USER_ID), eq(EntitlementType.JUNIOR), any());

		assertThatThrownBy(() -> graduationReviewer.review(pending.getId(), new GraduationReviewRequest("APPROVED", null)))
				.isInstanceOf(EntitlementProviderUnavailableException.class);
		topicSeeder.flushAndClear();
		TopicGraduation stillPending = topicGraduationRepository.findById(pending.getId()).orElseThrow();
		assertThat(stillPending.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
		assertThat(stillPending.getRewardExpiresAt()).isNull();

		willDoNothing().given(promotionalEntitlementGranter).grant(eq(USER_ID), eq(EntitlementType.JUNIOR), any());
		TopicGraduation retried = graduationReviewer.review(pending.getId(), new GraduationReviewRequest("APPROVED", null));

		assertThat(retried.isApproved()).isTrue();
		then(promotionalEntitlementGranter).should(times(2)).grant(eq(USER_ID), eq(EntitlementType.PUPPY), any());
		then(promotionalEntitlementGranter).should(times(1)).grant(eq(USER_ID), eq(EntitlementType.SENIOR), any());
	}

	@Test
	void 보완을_요청하면_메모와_함께_보완_요청_상태가_되고_이용권은_지급하지_않는다() {
		TopicGraduation pending = submit("분리불안");

		graduationReviewer.review(pending.getId(), new GraduationReviewRequest("REVISION_REQUESTED", "현관 장면부터 다시 찍어 주세요."));
		topicSeeder.flushAndClear();

		TopicGraduation saved = topicGraduationRepository.findById(pending.getId()).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(GraduationStatus.REVISION_REQUESTED);
		assertThat(saved.getReviewNote()).isEqualTo("현관 장면부터 다시 찍어 주세요.");
		assertThat(saved.getReviewedAt()).isNotNull();
		then(promotionalEntitlementGranter).should(never()).grant(any(), any(), any());
	}

	@Test
	void 보완_요청에_메모가_없으면_예외를_던진다() {
		TopicGraduation pending = submit("분리불안");

		assertThatThrownBy(() -> graduationReviewer.review(pending.getId(), new GraduationReviewRequest("REVISION_REQUESTED", " ")))
				.isInstanceOf(ReviewNoteRequiredException.class);
	}

	@Test
	void 이미_판정한_수료를_다시_판정하면_예외를_던진다() {
		TopicGraduation pending = submit("분리불안");
		graduationReviewer.review(pending.getId(), new GraduationReviewRequest("REVISION_REQUESTED", "다시 찍어 주세요."));
		topicSeeder.flushAndClear();

		assertThatThrownBy(() -> graduationReviewer.review(pending.getId(), new GraduationReviewRequest("APPROVED", null)))
				.isInstanceOf(GraduationNotPendingException.class);
	}

	@Test
	void 존재하지_않는_수료를_판정하면_예외를_던진다() {
		assertThatThrownBy(() -> graduationReviewer.review(999L, new GraduationReviewRequest("APPROVED", null)))
				.isInstanceOf(GraduationNotFoundException.class);
	}

	@Test
	void 지원하지_않는_판정이면_예외를_던진다() {
		TopicGraduation pending = submit("분리불안");

		assertThatThrownBy(() -> graduationReviewer.review(pending.getId(), new GraduationReviewRequest("REJECTED", "메모")))
				.isInstanceOf(InvalidReviewDecisionException.class);
	}

	private TopicGraduation submit(String title) {
		Topic topic = topicSeeder.completedTopic(USER_ID, title);
		TopicGraduation graduation = graduationSubmitter.submit(USER_ID, topic.getId(),
				new GraduationSubmissionRequest("videos/graduation/42/" + topic.getId() + ".mp4"));
		topicSeeder.flushAndClear();
		return graduation;
	}
}
