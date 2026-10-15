package com.daesabu.meongcoach.plan.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompleteRequest;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCompletionResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@ApplicationTest
@Import(FixedClockPlanConfig.class)
class PlanClockBoundaryTest extends PlanApplicationTestSupport {

	private static final LocalDate SEOUL_DATE = LocalDate.of(2026, 10, 15);

	@Autowired
	private PlanCreator planCreator;

	@Autowired
	private PlanFinder planFinder;

	@Autowired
	private PlanCompleter planCompleter;

	@Test
	void UTC로는_전날인_자정_직후에도_서울_날짜로_시작일을_저장한다() {
		PlanDetailResult result = planCreator.create(userId, manualRequest(false, sitTopicId));

		assertThat(result.startedOn()).isEqualTo(SEOUL_DATE);
		assertThat(result.targetEndOn()).isEqualTo(SEOUL_DATE.plusDays(13));
		assertThat(result.daysLeft()).isEqualTo(13);
		assertThat(result.expectedWeekNo()).isEqualTo(1);
	}

	@Test
	void 조회는_서울_날짜를_오늘로_계산한다() {
		planCreator.create(userId, manualRequest(false, sitTopicId));
		entityManager.flush();
		entityManager.clear();

		PlanDetailResult result = planFinder.findCurrent(userId).plan();

		assertThat(result.daysLeft()).isEqualTo(13);
	}

	@Test
	void 마무리는_서울_시각으로_완료_시각과_걸린_일수를_계산한다() {
		PlanDetailResult created = planCreator.create(userId, manualRequest(false, sitTopicId));

		PlanCompletionResult result = planCompleter.complete(userId, created.id(), new PlanCompleteRequest("ACHIEVED"));

		TrainingPlan plan = trainingPlanRepository.findById(created.id()).orElseThrow();
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(plan.getCompletedAt()).isEqualTo(LocalDateTime.of(2026, 10, 15, 0, 30));
		assertThat(result.daysTaken()).isEqualTo(1);
	}
}
