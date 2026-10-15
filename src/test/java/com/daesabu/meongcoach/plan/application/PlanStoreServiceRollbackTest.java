package com.daesabu.meongcoach.plan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.plan.application.FallbackPlanBuilder.FallbackResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.application.required.TrainingPlanRepository;
import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.plan.domain.TrainingPlanCreateCommand;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@NonTransactionalApplicationTest
class PlanStoreServiceRollbackTest {

	private static final Long USER_ID = 880001L;
	private static final Long ABSENT_DOG_ID = 880099L;

	@Autowired
	private PlanStoreService planStoreService;

	@Autowired
	private TrainingPlanRepository trainingPlanRepository;

	@AfterEach
	void tearDown() {
		trainingPlanRepository.deleteAll();
	}

	@Test
	void 기대사항_갱신이_실패하면_새_플랜을_저장하지_않고_기존_플랜도_포기하지_않는다() {
		PlanDraft draft = new PlanDraft("제목", "목표", "이유",
				List.of(new WeekDraft(1, "테마", "마일스톤", List.of(1L))));
		TrainingPlan active = trainingPlanRepository.save(TrainingPlan.create(new TrainingPlanCreateCommand(
				USER_ID, 1L, PlanMode.MANUAL, PlanGeneratorType.MANUAL, null, 1, 10, 3, LocalDate.now(), "{}",
				draft)));
		PlanCreateRequest request = new PlanCreateRequest(ABSENT_DOG_ID, "AI", null, "산책 예절을 배우고 싶어요", 10, 3,
				true);
		FallbackResult result = new FallbackResult(draft, 1, null, PlanGeneratorType.FALLBACK);

		assertThatThrownBy(() -> planStoreService.store(USER_ID, PlanMode.AI, request, result, "{}"))
				.isInstanceOf(DogNotFoundException.class);

		List<TrainingPlan> plans = trainingPlanRepository.findAll();
		assertThat(plans).hasSize(1);
		assertThat(plans.getFirst().getId()).isEqualTo(active.getId());
		assertThat(plans.getFirst().getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(trainingPlanRepository.existsByUserIdAndStatus(USER_ID, PlanStatus.ABANDONED)).isFalse();
	}
}
