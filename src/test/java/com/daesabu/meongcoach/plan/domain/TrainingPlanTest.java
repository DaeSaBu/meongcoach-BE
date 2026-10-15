package com.daesabu.meongcoach.plan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotActiveException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrainingPlanTest {

	private static final LocalDate STARTED_ON = LocalDate.of(2026, 10, 1);

	@Test
	void 플랜을_만들면_진행_중_상태로_시작한다() {
		TrainingPlan plan = TrainingPlan.create(command(2, draft("제목", "목표", "이유")));

		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(plan.getUserId()).isEqualTo(1L);
		assertThat(plan.getDogId()).isEqualTo(2L);
		assertThat(plan.getMode()).isEqualTo(PlanMode.AI);
		assertThat(plan.getGenerator()).isEqualTo(PlanGeneratorType.AI);
		assertThat(plan.getCompletedAt()).isNull();
		assertThat(plan.getOutcome()).isNull();
	}

	@Test
	void 목표_종료일은_시작일에서_주차_수만큼_더한_마지막_날이다() {
		TrainingPlan twoWeeks = TrainingPlan.create(command(2, draft("제목", "목표", "이유")));
		TrainingPlan oneWeek = TrainingPlan.create(command(1, draft("제목", "목표", "이유")));

		assertThat(twoWeeks.getTargetEndOn()).isEqualTo(LocalDate.of(2026, 10, 14));
		assertThat(oneWeek.getTargetEndOn()).isEqualTo(LocalDate.of(2026, 10, 7));
	}

	@Test
	void 문자열은_컬럼_길이에_맞게_자른다() {
		PlanDraft draft = new PlanDraft("가".repeat(30), "나".repeat(80), "다".repeat(200),
				List.of(new WeekDraft(1, "라".repeat(30), "마".repeat(60), List.of(10L))));

		TrainingPlan plan = TrainingPlan.create(command(1, draft));

		assertThat(plan.getTitle()).hasSize(20);
		assertThat(plan.getGoal()).hasSize(60);
		assertThat(plan.getReason()).hasSize(150);
		assertThat(plan.getWeeks().getFirst().getTheme()).hasSize(20);
		assertThat(plan.getWeeks().getFirst().getMilestone()).hasSize(40);
	}

	@Test
	void 주차와_항목을_초안_순서대로_만든다() {
		PlanDraft draft = new PlanDraft("제목", "목표", "이유", List.of(
				new WeekDraft(1, "앉아", "앉아까지 해내기", List.of(10L, 11L)),
				new WeekDraft(2, "기다려", "기다려까지 해내기", List.of(20L))));

		TrainingPlan plan = TrainingPlan.create(command(2, draft));

		assertThat(plan.getWeeks()).extracting(TrainingPlanWeek::getWeekNo).containsExactly(1, 2);
		assertThat(plan.getWeeks().get(1).getTheme()).isEqualTo("기다려");
		assertThat(plan.getWeeks().get(1).getMilestone()).isEqualTo("기다려까지 해내기");
		assertThat(plan.getItems()).extracting(TrainingPlanItem::getCurriculumId).containsExactly(10L, 11L, 20L);
		assertThat(plan.getItems()).extracting(TrainingPlanItem::getSortOrder).containsExactly(1, 2, 1);
		assertThat(plan.getItems()).extracting(TrainingPlanItem::getWeekNo).containsExactly(1, 1, 2);
	}

	@Test
	void 커리큘럼_ID는_주차와_순서대로_반환한다() {
		PlanDraft draft = new PlanDraft("제목", "목표", "이유", List.of(
				new WeekDraft(1, "a", "b", List.of(10L, 11L)),
				new WeekDraft(2, "c", "d", List.of(20L, 21L))));
		TrainingPlan plan = TrainingPlan.create(command(2, draft));

		assertThat(plan.curriculumIds()).containsExactly(10L, 11L, 20L, 21L);
	}

	@Test
	void 주차별_항목을_순서대로_반환한다() {
		PlanDraft draft = new PlanDraft("제목", "목표", "이유", List.of(
				new WeekDraft(1, "a", "b", List.of(10L, 11L)),
				new WeekDraft(2, "c", "d", List.of(20L))));
		TrainingPlan plan = TrainingPlan.create(command(2, draft));

		assertThat(plan.itemsOf(1)).extracting(TrainingPlanItem::getCurriculumId).containsExactly(10L, 11L);
		assertThat(plan.itemsOf(2)).extracting(TrainingPlanItem::getCurriculumId).containsExactly(20L);
		assertThat(plan.itemsOf(3)).isEmpty();
	}

	@Test
	void 포기하면_포기_상태가_된다() {
		TrainingPlan plan = TrainingPlan.create(command(2, draft("제목", "목표", "이유")));

		plan.abandon();

		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ABANDONED);
	}

	@Test
	void 완료하면_결과와_완료_시각을_기록한다() {
		TrainingPlan plan = TrainingPlan.create(command(2, draft("제목", "목표", "이유")));
		LocalDateTime now = LocalDateTime.of(2026, 10, 10, 9, 0);

		plan.complete(PlanOutcome.PARTIAL, now);

		assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(plan.getOutcome()).isEqualTo(PlanOutcome.PARTIAL);
		assertThat(plan.getCompletedAt()).isEqualTo(now);
	}

	@Test
	void 진행_중이_아닌_플랜은_완료할_수_없다() {
		TrainingPlan plan = TrainingPlan.create(command(2, draft("제목", "목표", "이유")));
		plan.complete(PlanOutcome.ACHIEVED, LocalDateTime.of(2026, 10, 10, 9, 0));

		assertThatThrownBy(() -> plan.complete(PlanOutcome.NOT_YET, LocalDateTime.of(2026, 10, 11, 9, 0)))
				.isInstanceOf(PlanNotActiveException.class);
	}

	@Test
	void 포기한_플랜은_완료할_수_없다() {
		TrainingPlan plan = TrainingPlan.create(command(2, draft("제목", "목표", "이유")));
		plan.abandon();

		assertThatThrownBy(() -> plan.complete(PlanOutcome.ACHIEVED, LocalDateTime.of(2026, 10, 10, 9, 0)))
				.isInstanceOf(PlanNotActiveException.class);
	}

	private static PlanDraft draft(String title, String goal, String reason) {
		return new PlanDraft(title, goal, reason, List.of(
				new WeekDraft(1, "테마1", "마일스톤1", List.of(10L)),
				new WeekDraft(2, "테마2", "마일스톤2", List.of(20L))));
	}

	private static TrainingPlanCreateCommand command(int durationWeeks, PlanDraft draft) {
		return new TrainingPlanCreateCommand(1L, 2L, PlanMode.AI, PlanGeneratorType.AI, null, durationWeeks, 10, 3,
				STARTED_ON, "{}", draft);
	}
}
