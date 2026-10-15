package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.plan.application.provided.dto.CurrentPlanResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import com.daesabu.meongcoach.plan.domain.PlanOutcome;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.progress.application.provided.LessonProgressUpdater;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.domain.shared.CurriculumStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class PlanFinderTest extends PlanApplicationTestSupport {

	@Autowired
	private PlanFinder planFinder;

	@Autowired
	private PlanCreator planCreator;

	@Autowired
	private LessonProgressUpdater lessonProgressUpdater;

	@Test
	void 플랜이_없으면_플랜은_null이고_완료_이력도_없다() {
		CurrentPlanResult result = planFinder.findCurrent(userId);

		assertThat(result.plan()).isNull();
		assertThat(result.hasCompletedPlan()).isFalse();
	}

	@Test
	void 진행_중_플랜이_있으면_상세를_반환한다() {
		planCreator.create(userId, manualRequest(false, sitTopicId));

		CurrentPlanResult result = planFinder.findCurrent(userId);

		assertThat(result.hasCompletedPlan()).isFalse();
		PlanDetailResult plan = result.plan();
		assertThat(plan.dogName()).isEqualTo("초코");
		assertThat(plan.currentWeekNo()).isEqualTo(1);
		assertThat(plan.progress().totalItems()).isEqualTo(2);
		assertThat(plan.progress().completedItems()).isZero();
		assertThat(plan.today().curriculumId()).isEqualTo(sitBasicId);
		assertThat(plan.today().lessonId()).isEqualTo(sitBasicLessonIds.getFirst());
		assertThat(plan.weeks()).extracting(PlanDetailResult.Week::status)
				.containsExactly(CurriculumStatus.NOT_STARTED, CurriculumStatus.NOT_STARTED);
	}

	@Test
	void 완료한_플랜만_있으면_플랜은_null이고_완료_이력이_있다() {
		planCreator.create(userId, manualRequest(false, sitTopicId));
		plansOf(PlanStatus.ACTIVE).getFirst().complete(PlanOutcome.NOT_YET, LocalDateTime.now());

		CurrentPlanResult result = planFinder.findCurrent(userId);

		assertThat(result.plan()).isNull();
		assertThat(result.hasCompletedPlan()).isTrue();
	}

	@Test
	void 포기한_플랜은_완료_이력으로_세지_않는다() {
		planCreator.create(userId, manualRequest(false, sitTopicId));
		planCreator.create(userId, manualRequest(true, comeTopicId));
		plansOf(PlanStatus.ACTIVE).getFirst().complete(PlanOutcome.ACHIEVED, LocalDateTime.now());

		assertThat(planFinder.findCurrent(userId).hasCompletedPlan()).isTrue();
		assertThat(plansOf(PlanStatus.ABANDONED)).hasSize(1);
	}

	@Test
	void 레슨을_완료하면_주차와_오늘_할_일이_다음으로_넘어간다() {
		planCreator.create(userId, manualRequest(false, sitTopicId));
		sitBasicLessonIds.forEach(lessonId -> lessonProgressUpdater.updateCompletion(userId, lessonId));
		entityManager.flush();
		entityManager.clear();

		PlanDetailResult plan = planFinder.findCurrent(userId).plan();

		assertThat(plan.currentWeekNo()).isEqualTo(2);
		assertThat(plan.progress().completedItems()).isEqualTo(1);
		assertThat(plan.today().curriculumId()).isEqualTo(sitAdvancedId);
		assertThat(plan.today().lessonId()).isEqualTo(sitAdvancedLessonIds.getFirst());
		assertThat(plan.weeks().getFirst().status()).isEqualTo(CurriculumStatus.COMPLETED);
		assertThat(plan.weeks().getFirst().items().getFirst().completedLessons()).isEqualTo(2);
	}

	@Test
	void 모든_레슨을_완료하면_오늘_할_일은_null이다() {
		planCreator.create(userId, manualRequest(false, sitTopicId));
		sitBasicLessonIds.forEach(lessonId -> lessonProgressUpdater.updateCompletion(userId, lessonId));
		sitAdvancedLessonIds.forEach(lessonId -> lessonProgressUpdater.updateCompletion(userId, lessonId));
		entityManager.flush();
		entityManager.clear();

		PlanDetailResult plan = planFinder.findCurrent(userId).plan();

		assertThat(plan.today()).isNull();
		assertThat(plan.progress().completedItems()).isEqualTo(2);
	}
}
