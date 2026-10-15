package com.daesabu.meongcoach.plan.application;

import static com.daesabu.meongcoach.plan.application.CatalogBuilder.TODAY;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.catalog;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.category;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.completed;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.curriculum;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.topic;
import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult.Item;
import com.daesabu.meongcoach.plan.application.provided.dto.PlanDetailResult.Week;
import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.TrainingPlan;
import com.daesabu.meongcoach.plan.domain.TrainingPlanCreateCommand;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.training.domain.shared.CurriculumStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanDetailAssemblerTest {

	private static final TrainingCatalogResult CATALOG = catalog(category(1L, EntitlementType.PUPPY,
			topic(1L, "앉기", curriculum(11L, "앉기 기초", 1, 2), curriculum(12L, "앉기 심화", 2, 1)),
			topic(2L, "산책", curriculum(21L, "산책 기초", 1, 1))));

	@Test
	void 아직_시작하지_않은_플랜은_첫_커리큘럼의_첫_레슨을_오늘의_할_일로_한다() {
		PlanDetailResult result = assemble(plan(TODAY), Set.of(), TODAY);

		assertThat(result.currentWeekNo()).isEqualTo(1);
		assertThat(result.progress().completedItems()).isZero();
		assertThat(result.progress().totalItems()).isEqualTo(3);
		assertThat(result.today().curriculumId()).isEqualTo(11L);
		assertThat(result.today().curriculumTitle()).isEqualTo("앉기 기초");
		assertThat(result.today().topicId()).isEqualTo(1L);
		assertThat(result.today().topicTitle()).isEqualTo("앉기");
		assertThat(result.today().lessonId()).isEqualTo(111L);
		assertThat(result.today().lessonTitle()).isEqualTo("앉기 기초 레슨1");
		assertThat(result.today().estimatedMinutes()).isEqualTo(5);
		assertThat(result.weeks()).extracting(Week::status)
				.containsExactly(CurriculumStatus.NOT_STARTED, CurriculumStatus.NOT_STARTED);
	}

	@Test
	void 플랜_정보와_주차_항목을_그대로_옮긴다() {
		PlanDetailResult result = assemble(plan(TODAY), Set.of(111L), TODAY);

		assertThat(result.dogId()).isEqualTo(2L);
		assertThat(result.dogName()).isEqualTo("초코");
		assertThat(result.mode()).isEqualTo(PlanMode.AI);
		assertThat(result.generator()).isEqualTo(PlanGeneratorType.AI);
		assertThat(result.title()).isEqualTo("제목");
		assertThat(result.durationWeeks()).isEqualTo(2);
		assertThat(result.dailyMinutes()).isEqualTo(10);
		assertThat(result.daysPerWeek()).isEqualTo(3);
		assertThat(result.startedOn()).isEqualTo(TODAY);
		assertThat(result.targetEndOn()).isEqualTo(TODAY.plusDays(13));
		assertThat(result.weeks()).extracting(Week::theme).containsExactly("테마1", "테마2");
		assertThat(result.weeks()).extracting(Week::milestone).containsExactly("마일스톤1", "마일스톤2");
		Item first = result.weeks().getFirst().items().getFirst();
		assertThat(first.curriculumId()).isEqualTo(11L);
		assertThat(first.topicId()).isEqualTo(1L);
		assertThat(first.topicTitle()).isEqualTo("앉기");
		assertThat(first.totalLessons()).isEqualTo(2);
		assertThat(first.completedLessons()).isEqualTo(1);
		assertThat(first.status()).isEqualTo(CurriculumStatus.IN_PROGRESS);
	}

	@Test
	void 일부_레슨을_끝낸_주차는_진행_중이고_다음_레슨이_오늘의_할_일이다() {
		PlanDetailResult result = assemble(plan(TODAY), Set.of(111L), TODAY);

		assertThat(result.weeks().getFirst().status()).isEqualTo(CurriculumStatus.IN_PROGRESS);
		assertThat(result.today().curriculumId()).isEqualTo(11L);
		assertThat(result.today().lessonId()).isEqualTo(112L);
	}

	@Test
	void 첫_커리큘럼을_끝내면_같은_주차의_다음_커리큘럼이_오늘의_할_일이다() {
		PlanDetailResult result = assemble(plan(TODAY), completed(111L, 112L), TODAY);

		assertThat(result.currentWeekNo()).isEqualTo(1);
		assertThat(result.progress().completedItems()).isEqualTo(1);
		assertThat(result.today().curriculumId()).isEqualTo(12L);
		assertThat(result.today().lessonId()).isEqualTo(121L);
	}

	@Test
	void 첫_주차를_모두_끝내면_다음_주차가_현재_주차다() {
		PlanDetailResult result = assemble(plan(TODAY), completed(111L, 112L, 121L), TODAY);

		assertThat(result.currentWeekNo()).isEqualTo(2);
		assertThat(result.weeks().getFirst().status()).isEqualTo(CurriculumStatus.COMPLETED);
		assertThat(result.weeks().get(1).status()).isEqualTo(CurriculumStatus.NOT_STARTED);
		assertThat(result.today().curriculumId()).isEqualTo(21L);
		assertThat(result.today().lessonId()).isEqualTo(211L);
	}

	@Test
	void 모두_끝내면_마지막_주차가_현재_주차이고_오늘의_할_일은_없다() {
		PlanDetailResult result = assemble(plan(TODAY), completed(111L, 112L, 121L, 211L), TODAY);

		assertThat(result.currentWeekNo()).isEqualTo(2);
		assertThat(result.today()).isNull();
		assertThat(result.progress().completedItems()).isEqualTo(3);
		assertThat(result.progress().totalItems()).isEqualTo(3);
		assertThat(result.weeks()).extracting(Week::status)
				.containsExactly(CurriculumStatus.COMPLETED, CurriculumStatus.COMPLETED);
	}

	@Test
	void 둘째_주차만_끝내도_첫_주차가_남아_있으면_첫_주차가_현재_주차다() {
		PlanDetailResult result = assemble(plan(TODAY), completed(211L), TODAY);

		assertThat(result.currentWeekNo()).isEqualTo(1);
		assertThat(result.weeks().get(1).status()).isEqualTo(CurriculumStatus.COMPLETED);
	}

	@Test
	void 시작일로부터_지난_주수로_예상_주차를_구한다() {
		assertThat(assemble(plan(TODAY), Set.of(), TODAY).expectedWeekNo()).isEqualTo(1);
		assertThat(assemble(plan(TODAY), Set.of(), TODAY.plusDays(6)).expectedWeekNo()).isEqualTo(1);
		assertThat(assemble(plan(TODAY), Set.of(), TODAY.plusDays(7)).expectedWeekNo()).isEqualTo(2);
	}

	@Test
	void 예상_주차는_1주차와_전체_주차_사이로_맞춘다() {
		assertThat(assemble(plan(TODAY), Set.of(), TODAY.plusDays(40)).expectedWeekNo()).isEqualTo(2);
		assertThat(assemble(plan(TODAY), Set.of(), TODAY.minusDays(3)).expectedWeekNo()).isEqualTo(1);
	}

	@Test
	void 남은_일수는_오늘부터_목표_종료일까지이고_지나면_음수다() {
		assertThat(assemble(plan(TODAY), Set.of(), TODAY).daysLeft()).isEqualTo(13);
		assertThat(assemble(plan(TODAY), Set.of(), TODAY.plusDays(13)).daysLeft()).isZero();
		assertThat(assemble(plan(TODAY), Set.of(), TODAY.plusDays(16)).daysLeft()).isEqualTo(-3);
	}

	@Test
	void 카탈로그에_없는_커리큘럼은_제외한다() {
		TrainingPlan plan = plan(TODAY, new WeekDraft(1, "테마1", "마일스톤1", List.of(11L, 99L)),
				new WeekDraft(2, "테마2", "마일스톤2", List.of(21L)));

		PlanDetailResult result = assemble(plan, Set.of(), TODAY);

		assertThat(result.weeks().getFirst().items()).extracting(Item::curriculumId).containsExactly(11L);
		assertThat(result.progress().totalItems()).isEqualTo(2);
	}

	@Test
	void 항목이_모두_카탈로그에_없으면_진행률은_0_대_0이다() {
		TrainingPlan plan = plan(TODAY, new WeekDraft(1, "테마1", "마일스톤1", List.of(98L)),
				new WeekDraft(2, "테마2", "마일스톤2", List.of(99L)));

		PlanDetailResult result = assemble(plan, Set.of(), TODAY);

		assertThat(result.progress().completedItems()).isZero();
		assertThat(result.progress().totalItems()).isZero();
		assertThat(result.today()).isNull();
		assertThat(result.weeks()).extracting(Week::status)
				.containsExactly(CurriculumStatus.NOT_STARTED, CurriculumStatus.NOT_STARTED);
		assertThat(result.weeks()).allSatisfy(week -> assertThat(week.items()).isEmpty());
	}

	private static PlanDetailResult assemble(TrainingPlan plan, Set<Long> completedLessonIds, LocalDate today) {
		return PlanDetailAssembler.assemble(plan, "초코", CATALOG, completedLessonIds, today);
	}

	private static TrainingPlan plan(LocalDate startedOn) {
		return plan(startedOn, new WeekDraft(1, "테마1", "마일스톤1", List.of(11L, 12L)),
				new WeekDraft(2, "테마2", "마일스톤2", List.of(21L)));
	}

	private static TrainingPlan plan(LocalDate startedOn, WeekDraft... weeks) {
		PlanDraft draft = new PlanDraft("제목", "목표", "이유", List.of(weeks));
		return TrainingPlan.create(new TrainingPlanCreateCommand(1L, 2L, PlanMode.AI, PlanGeneratorType.AI, null, 2, 10,
				3, startedOn, "{}", draft));
	}
}
