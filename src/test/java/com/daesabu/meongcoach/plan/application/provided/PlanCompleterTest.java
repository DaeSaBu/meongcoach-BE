package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompleteRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompletionResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompletionResult.ReflectedTopic;
import com.daesabu.meongcoach.plan.domain.PlanOutcome;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.plan.domain.exception.PlanInvalidOutcomeException;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotActiveException;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotFoundException;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressUpdater;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.user.domain.UserProfile;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@ApplicationTest
class PlanCompleterTest extends PlanApplicationTestSupport {

	@Autowired
	private PlanCompleter planCompleter;

	@Autowired
	private PlanCreator planCreator;

	@Autowired
	private LessonProgressUpdater lessonProgressUpdater;

	@Test
	void 달성으로_마무리하면_플랜_토픽이_배운_토픽에_누적된다() {
		Long planId = createManualPlan(sitTopicId, comeTopicId);

		PlanCompletionResult result = planCompleter.complete(userId, planId, new PlanCompleteRequest("ACHIEVED"));

		assertThat(result.outcome()).isEqualTo(PlanOutcome.ACHIEVED);
		assertThat(result.reflectedTopics()).extracting(ReflectedTopic::topicId)
				.containsExactly(sitTopicId, comeTopicId);
		assertThat(result.reflectedTopics()).extracting(ReflectedTopic::topicTitle)
				.containsExactly("앉아", "이리와");
		assertThat(priorTopicIds()).containsExactlyInAnyOrder(PRIOR_TOPIC_ID, sitTopicId, comeTopicId);
	}

	@Test
	void 같은_토픽의_커리큘럼이_여럿이어도_토픽은_한_번만_반영한다() {
		Long planId = createManualPlan(sitTopicId);

		PlanCompletionResult result = planCompleter.complete(userId, planId, new PlanCompleteRequest("ACHIEVED"));

		assertThat(result.totalCurriculums()).isEqualTo(2);
		assertThat(result.reflectedTopics()).hasSize(1);
		assertThat(result.reflectedTopics().getFirst().topicId()).isEqualTo(sitTopicId);
	}

	@Test
	void 일부_달성으로_마무리해도_배운_토픽에_누적된다() {
		Long planId = createManualPlan(sitTopicId);

		planCompleter.complete(userId, planId, new PlanCompleteRequest("PARTIAL"));

		assertThat(priorTopicIds()).containsExactlyInAnyOrder(PRIOR_TOPIC_ID, sitTopicId);
	}

	@Test
	void 아직이라고_마무리하면_배운_토픽에_반영하지_않는다() {
		Long planId = createManualPlan(sitTopicId);

		PlanCompletionResult result = planCompleter.complete(userId, planId, new PlanCompleteRequest("NOT_YET"));

		assertThat(result.reflectedTopics()).isEmpty();
		assertThat(priorTopicIds()).containsExactly(PRIOR_TOPIC_ID);
	}

	@Test
	void 마무리하면_플랜이_완료_상태가_되고_결과가_저장된다() {
		Long planId = createManualPlan(sitTopicId);

		planCompleter.complete(userId, planId, new PlanCompleteRequest("PARTIAL"));

		TrainingPlan plan = trainingPlanRepository.findById(planId).orElseThrow();
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(plan.getOutcome()).isEqualTo(PlanOutcome.PARTIAL);
		assertThat(plan.getCompletedAt()).isNotNull();
	}

	@Test
	void 진행하지_않은_채로도_마무리하며_커리큘럼과_레슨_수를_집계한다() {
		Long planId = createManualPlan(sitTopicId);
		sitBasicLessonIds.forEach(lessonId -> lessonProgressUpdater.updateCompletion(userId, lessonId));

		PlanCompletionResult result = planCompleter.complete(userId, planId, new PlanCompleteRequest("PARTIAL"));

		assertThat(result.planId()).isEqualTo(planId);
		assertThat(result.title()).isNotBlank();
		assertThat(result.completedCurriculums()).isEqualTo(1);
		assertThat(result.totalCurriculums()).isEqualTo(2);
		assertThat(result.totalLessons()).isEqualTo(4);
	}

	@Test
	void 걸린_일수는_시작일부터_오늘까지_포함해_센다() {
		Long planId = createManualPlan(sitTopicId);
		TrainingPlan plan = trainingPlanRepository.findById(planId).orElseThrow();
		ReflectionTestUtils.setField(plan, "startedOn", LocalDate.now().minusDays(4));

		PlanCompletionResult result = planCompleter.complete(userId, planId, new PlanCompleteRequest("ACHIEVED"));

		assertThat(result.daysTaken()).isEqualTo(5);
	}

	@Test
	void 시작한_날_마무리하면_걸린_일수는_하루다() {
		Long planId = createManualPlan(sitTopicId);

		PlanCompletionResult result = planCompleter.complete(userId, planId, new PlanCompleteRequest("ACHIEVED"));

		assertThat(result.daysTaken()).isEqualTo(1);
	}

	@Test
	void 없는_플랜이면_찾을_수_없다() {
		assertThatThrownBy(() -> planCompleter.complete(userId, 123456L, new PlanCompleteRequest("ACHIEVED")))
				.isInstanceOf(PlanNotFoundException.class);
	}

	@Test
	void 다른_사용자의_플랜이면_찾을_수_없다() {
		Long planId = createManualPlan(sitTopicId);

		assertThatThrownBy(() -> planCompleter.complete(userId + 1000, planId, new PlanCompleteRequest("ACHIEVED")))
				.isInstanceOf(PlanNotFoundException.class);
	}

	@Test
	void 이미_마무리한_플랜이면_충돌한다() {
		Long planId = createManualPlan(sitTopicId);
		planCompleter.complete(userId, planId, new PlanCompleteRequest("NOT_YET"));

		assertThatThrownBy(() -> planCompleter.complete(userId, planId, new PlanCompleteRequest("ACHIEVED")))
				.isInstanceOf(PlanNotActiveException.class);
		assertThat(priorTopicIds()).containsExactly(PRIOR_TOPIC_ID);
	}

	@Test
	void 알_수_없는_결과면_거절한다() {
		Long planId = createManualPlan(sitTopicId);

		assertThatThrownBy(() -> planCompleter.complete(userId, planId, new PlanCompleteRequest("WRONG")))
				.isInstanceOf(PlanInvalidOutcomeException.class);
	}

	private Long createManualPlan(Long... topicIds) {
		return planCreator.create(userId, manualRequest(false, topicIds)).id();
	}

	private Set<Long> priorTopicIds() {
		UserProfile profile = userProfileRepository.findById(userId).orElseThrow();
		return profile.getPriorTrainingTopicIds();
	}
}
