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
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import com.daesabu.meongcoach.graduation.domain.exception.GraduationNotApprovedException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.Topic;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@ApplicationTest
@Import(TopicSeeder.class)
class GraduationRewardClaimerTest {

	private static final Long USER_ID = 42L;
	private static final Duration THREE_DAYS = Duration.ofDays(3);

	@Autowired
	private GraduationRewardClaimer graduationRewardClaimer;

	@Autowired
	private GraduationSubmitter graduationSubmitter;

	@Autowired
	private GraduationReviewer graduationReviewer;

	@Autowired
	private TopicGraduationRepository topicGraduationRepository;

	@Autowired
	private PromotionalEntitlementGranter promotionalEntitlementGranter;

	@Autowired
	private TopicSeeder topicSeeder;

	@Test
	void 승인된_수료의_보상을_받으면_받은_시점부터_3일짜리_전체_이용권을_지급하고_기록한다() {
		Topic topic = approvedTopic("분리불안");
		Instant before = Instant.now();

		TopicGraduation graduation = graduationRewardClaimer.claim(USER_ID, topic.getId());

		Instant after = Instant.now();
		assertThat(graduation.getRewardClaimedAt()).isBetween(before, after);
		assertThat(graduation.getRewardExpiresAt()).isEqualTo(graduation.getRewardClaimedAt().plus(THREE_DAYS));
		for (EntitlementType type : EntitlementType.values()) {
			then(promotionalEntitlementGranter).should().grant(USER_ID, type, graduation.getRewardExpiresAt());
		}
	}

	@Test
	void 이전에_받은_보상_기간이_남아_있으면_그_뒤로_3일을_잇는다() {
		Topic first = approvedTopic("분리불안");
		Topic second = approvedTopic("산책");
		TopicGraduation firstReward = graduationRewardClaimer.claim(USER_ID, first.getId());
		topicSeeder.flushAndClear();

		TopicGraduation secondReward = graduationRewardClaimer.claim(USER_ID, second.getId());

		assertThat(secondReward.getRewardExpiresAt()).isEqualTo(firstReward.getRewardExpiresAt().plus(THREE_DAYS));
	}

	@Test
	void 이미_받은_보상을_다시_요청하면_지급하지_않고_저장된_값을_돌려준다() {
		Topic topic = approvedTopic("분리불안");
		TopicGraduation first = graduationRewardClaimer.claim(USER_ID, topic.getId());
		topicSeeder.flushAndClear();

		TopicGraduation second = graduationRewardClaimer.claim(USER_ID, topic.getId());

		assertThat(second.getRewardExpiresAt()).isEqualTo(first.getRewardExpiresAt());
		assertThat(second.getRewardClaimedAt()).isEqualTo(first.getRewardClaimedAt());
		then(promotionalEntitlementGranter).should(times(EntitlementType.values().length)).grant(any(), any(), any());
	}

	@Test
	void 승인되지_않은_수료의_보상을_요청하면_예외를_던진다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");
		graduationSubmitter.submit(USER_ID, topic.getId(), new GraduationSubmissionRequest("videos/graduation/42/a.mp4"));
		topicSeeder.flushAndClear();

		assertThatThrownBy(() -> graduationRewardClaimer.claim(USER_ID, topic.getId()))
				.isInstanceOf(GraduationNotApprovedException.class);
		then(promotionalEntitlementGranter).should(never()).grant(any(), any(), any());
	}

	@Test
	void 제출한_적이_없는_토픽의_보상을_요청하면_예외를_던진다() {
		Topic topic = topicSeeder.completedTopic(USER_ID, "분리불안");

		assertThatThrownBy(() -> graduationRewardClaimer.claim(USER_ID, topic.getId()))
				.isInstanceOf(GraduationNotApprovedException.class);
	}

	@Test
	void 지급_도중_실패하면_미수령으로_남고_다시_받으면_네_종류를_다시_지급한다() {
		Topic topic = approvedTopic("분리불안");
		willThrow(new EntitlementProviderUnavailableException())
				.given(promotionalEntitlementGranter).grant(eq(USER_ID), eq(EntitlementType.JUNIOR), any());

		assertThatThrownBy(() -> graduationRewardClaimer.claim(USER_ID, topic.getId()))
				.isInstanceOf(EntitlementProviderUnavailableException.class);
		topicSeeder.flushAndClear();
		assertThat(topicGraduationRepository.findByUserIdAndTopicId(USER_ID, topic.getId()).orElseThrow().isRewardClaimed())
				.isFalse();

		willDoNothing().given(promotionalEntitlementGranter).grant(eq(USER_ID), eq(EntitlementType.JUNIOR), any());
		TopicGraduation retried = graduationRewardClaimer.claim(USER_ID, topic.getId());

		assertThat(retried.isRewardClaimed()).isTrue();
		then(promotionalEntitlementGranter).should(times(2)).grant(eq(USER_ID), eq(EntitlementType.PUPPY), any());
		then(promotionalEntitlementGranter).should(times(1)).grant(eq(USER_ID), eq(EntitlementType.SENIOR), any());
	}

	private Topic approvedTopic(String title) {
		Topic topic = topicSeeder.completedTopic(USER_ID, title);
		GraduationSubmission submission = graduationSubmitter.submit(USER_ID, topic.getId(),
				new GraduationSubmissionRequest("videos/graduation/42/" + topic.getId() + ".mp4"));
		graduationReviewer.review(submission.getId(), new GraduationReviewRequest("APPROVED", null));
		topicSeeder.flushAndClear();
		return topic;
	}
}
