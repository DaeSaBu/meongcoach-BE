package com.daesabu.meongcoach.plan.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.PlanOutcome;
import com.daesabu.meongcoach.plan.domain.PlanStatus;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.plan.domain.TrainingPlanCreateCommand;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class TrainingPlanRepositoryTest {

	@Autowired
	private TrainingPlanRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 플랜을_저장하면_주차와_항목을_함께_조회한다() {
		TrainingPlan saved = repository.saveAndFlush(plan(1L));
		entityManager.clear();

		TrainingPlan found = repository.findByIdAndUserId(saved.getId(), 1L).orElseThrow();

		assertThat(found.getWeeks()).hasSize(2);
		assertThat(found.curriculumIds()).containsExactly(10L, 11L, 20L);
		assertThat(found.getInterviewJson()).isEqualTo("{}");
	}

	@Test
	void 다른_사용자의_플랜은_조회되지_않는다() {
		TrainingPlan saved = repository.saveAndFlush(plan(1L));

		assertThat(repository.findByIdAndUserId(saved.getId(), 2L)).isEmpty();
	}

	@Test
	void 사용자와_상태로_플랜을_조회한다() {
		repository.saveAndFlush(plan(1L));
		TrainingPlan completed = plan(1L);
		completed.complete(PlanOutcome.ACHIEVED, LocalDateTime.of(2026, 10, 10, 9, 0));
		repository.saveAndFlush(completed);
		repository.saveAndFlush(plan(2L));

		assertThat(repository.findByUserIdAndStatus(1L, PlanStatus.ACTIVE)).hasSize(1);
		assertThat(repository.findByUserIdAndStatus(1L, PlanStatus.COMPLETED)).hasSize(1);
		assertThat(repository.existsByUserIdAndStatus(1L, PlanStatus.ACTIVE)).isTrue();
		assertThat(repository.existsByUserIdAndStatus(1L, PlanStatus.ABANDONED)).isFalse();
	}

	private static TrainingPlan plan(Long userId) {
		PlanDraft draft = new PlanDraft("제목", "목표", "이유", List.of(
				new WeekDraft(1, "테마1", "마일스톤1", List.of(10L, 11L)),
				new WeekDraft(2, "테마2", "마일스톤2", List.of(20L))));
		return TrainingPlan.create(new TrainingPlanCreateCommand(userId, 2L, PlanMode.AI, PlanGeneratorType.AI, null,
				2, 10, 3, LocalDate.of(2026, 10, 1), "{}", draft));
	}
}
