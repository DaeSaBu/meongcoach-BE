package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.training.domain.exception.DuplicatePlanCurriculumException;
import com.daesabu.meongcoach.training.domain.exception.EmptyPlanCurriculumsException;
import com.daesabu.meongcoach.training.domain.exception.InvalidPlanCurriculumException;
import com.daesabu.meongcoach.training.domain.exception.PlanLessonOrderException;
import com.daesabu.meongcoach.training.domain.exception.PlanNotActiveException;
import com.daesabu.meongcoach.training.domain.exception.PlanNotCompletedException;
import com.daesabu.meongcoach.training.domain.exception.PlanOutcomeAlreadyRecordedException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

class PlanTest {

	private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

	@Test
	void 활성_플랜을_완료하면_완료_상태와_시각을_저장한다() {
		Plan plan = createPlan();
		Instant completedAt = Instant.parse("2026-10-23T12:00:00Z");

		plan.complete(completedAt);

		assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(plan.getCompletedAt()).isEqualTo(completedAt);
	}

	@Test
	void 활성_플랜을_교체하면_완료_시각_없이_교체_상태가_된다() {
		Plan plan = createPlan();

		plan.replace();

		assertThat(plan.getStatus()).isEqualTo(PlanStatus.REPLACED);
		assertThat(plan.getCompletedAt()).isNull();
	}

	@Test
	void 완료된_플랜은_다시_완료하거나_교체할_수_없다() {
		Plan plan = createPlan();
		Instant completedAt = Instant.parse("2026-10-23T12:00:00Z");
		plan.complete(completedAt);

		assertThatThrownBy(() -> plan.complete(completedAt.plusSeconds(86400)))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(plan::replace).isInstanceOf(IllegalStateException.class);
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(plan.getCompletedAt()).isEqualTo(completedAt);
	}

	@Test
	void 교체된_플랜은_완료하거나_다시_교체할_수_없다() {
		Plan plan = createPlan();
		plan.replace();
		Instant completedAt = Instant.parse("2026-10-23T12:00:00Z");

		assertThatThrownBy(() -> plan.complete(completedAt)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(plan::replace).isInstanceOf(IllegalStateException.class);
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.REPLACED);
		assertThat(plan.getCompletedAt()).isNull();
	}

	@Test
	void 완료_시각이_null이면_상태를_변경하지_않는다() {
		Plan plan = createPlan();

		assertThatThrownBy(() -> plan.complete(null)).isInstanceOf(NullPointerException.class);
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(plan.getCompletedAt()).isNull();
	}

	@Test
	void 활성_플랜에_완료_레슨을_중복_없이_기록한다() {
		Plan plan = createPlan();

		plan.completeLesson(101L);
		plan.completeLesson(102L);
		plan.completeLesson(101L);

		assertThat(plan.getCompletedLessonIds()).containsExactlyInAnyOrder(101L, 102L);
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
	}

	@Test
	void 아직_완료하지_않은_레슨은_완료_처리가_필요하다() {
		assertThat(createPlan().needsLessonCompletion(101L)).isTrue();
	}

	@Test
	void 이미_완료한_레슨은_플랜이_완료되어도_완료_처리가_필요_없다() {
		Plan plan = createPlan();
		plan.completeCurrentLesson(101L, List.of(List.of(101L)), Instant.parse("2026-10-25T12:00:00Z"));

		assertThat(plan.needsLessonCompletion(101L)).isFalse();
	}

	@Test
	void 완료된_플랜의_새_레슨은_거절한다() {
		Plan plan = createPlan();
		plan.complete(Instant.parse("2026-10-25T12:00:00Z"));

		assertThatThrownBy(() -> plan.needsLessonCompletion(101L)).isInstanceOf(PlanNotActiveException.class);
	}

	@Test
	void 교체된_플랜은_이미_완료한_레슨도_거절한다() {
		Plan plan = createPlan();
		plan.completeLesson(101L);
		plan.replace();

		assertThatThrownBy(() -> plan.needsLessonCompletion(101L)).isInstanceOf(PlanNotActiveException.class);
	}

	@Test
	void 토픽_안에서_현재_차례가_아닌_레슨은_거절하고_기록하지_않는다() {
		Plan plan = createPlan();
		Instant completedAt = Instant.parse("2026-10-25T12:00:00Z");

		assertThatThrownBy(() -> plan.completeCurrentLesson(102L, List.of(List.of(101L, 102L)), completedAt))
				.isInstanceOf(PlanLessonOrderException.class);
		assertThat(plan.getCompletedLessonIds()).isEmpty();
	}

	@Test
	void 남은_레슨이_없으면_어떤_레슨도_현재_레슨으로_완료할_수_없다() {
		Plan plan = createPlan();
		plan.completeLesson(101L);
		Instant completedAt = Instant.parse("2026-10-25T12:00:00Z");

		assertThatThrownBy(() -> plan.completeCurrentLesson(102L, List.of(List.of(101L)), completedAt))
				.isInstanceOf(PlanLessonOrderException.class);
	}

	@Test
	void 다른_토픽의_첫_미완료_레슨은_앞_토픽을_마치지_않아도_완료한다() {
		Plan plan = createPlan();
		List<List<Long>> topicLessonIds = List.of(List.of(101L, 102L), List.of(201L, 202L));
		Instant completedAt = Instant.parse("2026-10-25T12:00:00Z");

		plan.completeCurrentLesson(201L, topicLessonIds, completedAt);

		assertThat(plan.getCompletedLessonIds()).containsExactly(201L);
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
	}

	@Test
	void 마지막_레슨을_완료해야_플랜이_완료된다() {
		Plan plan = createPlan();
		List<List<Long>> orderedLessonIds = List.of(List.of(101L), List.of(102L));
		Instant completedAt = Instant.parse("2026-10-25T12:00:00Z");

		plan.completeCurrentLesson(101L, orderedLessonIds, completedAt);
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(plan.getCompletedAt()).isNull();

		plan.completeCurrentLesson(102L, orderedLessonIds, completedAt);
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(plan.getCompletedAt()).isEqualTo(completedAt);
	}

	@Test
	void 완료된_플랜에는_완료_레슨을_추가할_수_없다() {
		Plan plan = createPlan();
		plan.completeLesson(101L);
		plan.complete(Instant.parse("2026-10-25T12:00:00Z"));

		assertThatThrownBy(() -> plan.completeLesson(102L)).isInstanceOf(IllegalStateException.class);
		assertThat(plan.getCompletedLessonIds()).containsExactly(101L);
	}

	@Test
	void 교체된_플랜에는_완료_레슨을_추가할_수_없다() {
		Plan plan = createPlan();
		plan.replace();

		assertThatThrownBy(() -> plan.completeLesson(101L)).isInstanceOf(IllegalStateException.class);
		assertThat(plan.getCompletedLessonIds()).isEmpty();
	}

	@Test
	void 완료_레슨_ID가_null이면_기록하지_않는다() {
		Plan plan = createPlan();

		assertThatThrownBy(() -> plan.completeLesson(null)).isInstanceOf(NullPointerException.class);
		assertThat(plan.getCompletedLessonIds()).isEmpty();
	}

	@Test
	void 외부에서_완료_레슨_기록을_직접_변경할_수_없다() {
		Plan plan = createPlan();
		plan.completeLesson(101L);

		assertThatThrownBy(() -> plan.getCompletedLessonIds().clear())
				.isInstanceOf(UnsupportedOperationException.class);
		assertThat(plan.getCompletedLessonIds()).containsExactly(101L);
	}

	@Test
	void 현재_레슨을_모두_완료했는지_플랜의_기록으로_판단한다() {
		Plan plan = createPlan();
		List<Long> currentLessonIds = List.of(101L, 102L);

		assertThat(plan.hasCompletedAllLessons(currentLessonIds)).isFalse();
		plan.completeLesson(101L);
		assertThat(plan.hasCompletedAllLessons(currentLessonIds)).isFalse();
		plan.completeLesson(102L);
		assertThat(plan.hasCompletedAllLessons(currentLessonIds)).isTrue();
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(plan.getCompletedAt()).isNull();
	}

	@Test
	void 현재_목록에서_제외된_레슨의_완료_기록은_판단에_영향을_주지_않는다() {
		Plan plan = createPlan();
		plan.completeLesson(101L);
		plan.completeLesson(102L);

		assertThat(plan.hasCompletedAllLessons(List.of(102L))).isTrue();
		assertThat(plan.hasCompletedAllLessons(List.of(102L, 103L))).isFalse();
	}

	@Test
	void 현재_레슨_목록이_비어_있으면_완료_판단을_거절한다() {
		Plan plan = createPlan();

		assertThatThrownBy(() -> plan.hasCompletedAllLessons(List.of()))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 토픽마다_교육_순서의_첫_미완료_레슨을_찾는다() {
		Plan plan = createPlan();
		List<List<Long>> topicLessonIds = List.of(List.of(103L, 101L), List.of(102L));

		assertThat(plan.findCurrentLessonIds(topicLessonIds)).containsExactly(103L, 102L);
		plan.completeLesson(103L);
		assertThat(plan.findCurrentLessonIds(topicLessonIds)).containsExactly(101L, 102L);
		plan.completeLesson(102L);
		assertThat(plan.findCurrentLessonIds(topicLessonIds)).containsExactly(101L);
		plan.completeLesson(101L);
		assertThat(plan.findCurrentLessonIds(topicLessonIds)).isEmpty();
	}

	@Test
	void 교육_목록_앞에_새_레슨이_추가되면_그_레슨부터_진행한다() {
		Plan plan = createPlan();
		plan.completeLesson(101L);
		plan.completeLesson(102L);

		assertThat(plan.findCurrentLessonIds(List.of(List.of(104L, 102L, 103L)))).containsExactly(104L);
		plan.completeLesson(104L);
		assertThat(plan.findCurrentLessonIds(List.of(List.of(104L, 102L, 103L)))).containsExactly(103L);
	}

	@Test
	void 레슨_목록이_비어_있으면_현재_레슨_탐색을_거절한다() {
		Plan plan = createPlan();

		assertThatThrownBy(() -> plan.findCurrentLessonIds(List.of()))
				.isInstanceOf(IllegalStateException.class);
	}

	@ParameterizedTest
	@EnumSource(PlanOutcome.class)
	void 완료된_플랜에_회고_결과를_기록한다(PlanOutcome outcome) {
		Plan plan = createPlan();
		plan.complete(Instant.parse("2026-10-25T12:00:00Z"));

		plan.recordOutcome(outcome);

		assertThat(plan.getOutcome()).isEqualTo(outcome);
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
	}

	@Test
	void 활성_플랜에는_회고_결과를_기록할_수_없다() {
		Plan plan = createPlan();

		assertThatThrownBy(() -> plan.recordOutcome(PlanOutcome.ACHIEVED))
				.isInstanceOf(PlanNotCompletedException.class);
		assertThat(plan.getOutcome()).isNull();
	}

	@Test
	void 교체된_플랜에는_회고_결과를_기록할_수_없다() {
		Plan plan = createPlan();
		plan.replace();

		assertThatThrownBy(() -> plan.recordOutcome(PlanOutcome.ACHIEVED))
				.isInstanceOf(PlanNotCompletedException.class);
		assertThat(plan.getOutcome()).isNull();
	}

	@Test
	void 이미_회고_결과를_기록한_플랜은_다시_기록할_수_없다() {
		Plan plan = createPlan();
		plan.complete(Instant.parse("2026-10-25T12:00:00Z"));
		plan.recordOutcome(PlanOutcome.PARTIAL);

		assertThatThrownBy(() -> plan.recordOutcome(PlanOutcome.NOT_YET))
				.isInstanceOf(PlanOutcomeAlreadyRecordedException.class);
		assertThat(plan.getOutcome()).isEqualTo(PlanOutcome.PARTIAL);
	}

	@Test
	void 회고_결과가_null이면_기록하지_않는다() {
		Plan plan = createPlan();
		plan.complete(Instant.parse("2026-10-25T12:00:00Z"));

		assertThatThrownBy(() -> plan.recordOutcome(null)).isInstanceOf(NullPointerException.class);
		assertThat(plan.getOutcome()).isNull();
	}

	@ParameterizedTest(name = "{0}에 생성해 {1}에 완료하면 {2}일")
	@CsvSource({
			"2026-10-23T01:00:00Z, 2026-10-23T05:00:00Z, 1",
			"2026-10-23T01:00:00Z, 2026-10-23T15:30:00Z, 2",
			"2026-10-22T16:00:00Z, 2026-11-05T14:59:59Z, 14"
	})
	void 한국_날짜_기준으로_생성일부터_완료일까지_걸린_날을_센다(String createdAt, String completedAt, int expected) {
		Plan plan = createPlan();
		setCreatedAt(plan, Instant.parse(createdAt));
		plan.complete(Instant.parse(completedAt));

		int elapsedDays = plan.countElapsedDays(KOREA);

		assertThat(elapsedDays).isEqualTo(expected);
	}

	@Test
	void 완료되지_않은_플랜은_걸린_날을_셀_수_없다() {
		Plan plan = createPlan();
		setCreatedAt(plan, Instant.parse("2026-10-23T01:00:00Z"));

		assertThatThrownBy(() -> plan.countElapsedDays(KOREA)).isInstanceOf(IllegalStateException.class);
	}

	private void setCreatedAt(Plan plan, Instant createdAt) {
		LocalDateTime serverTime = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault());
		ReflectionTestUtils.setField(plan, "createdAt", serverTime);
	}

	private Plan createPlan() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표");
		return Plan.create(command, List.of(7L, 4L));
	}

	@Test
	void 커리큘럼_목록이_비어_있으면_생성을_거절한다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표");

		assertThatThrownBy(() -> Plan.create(command, List.of()))
				.isInstanceOf(EmptyPlanCurriculumsException.class);
	}

	@Test
	void 중복_커리큘럼이_있으면_제거하지_않고_생성을_거절한다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표");
		List<Long> curriculumIds = new ArrayList<>(List.of(7L, 4L, 7L));

		assertThatThrownBy(() -> Plan.create(command, curriculumIds))
				.isInstanceOf(DuplicatePlanCurriculumException.class);
		assertThat(curriculumIds).containsExactly(7L, 4L, 7L);
	}

	@Test
	void 커리큘럼_ID에_null이_있으면_생성을_거절한다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표");
		List<Long> curriculumIds = Arrays.asList(7L, null);

		assertThatThrownBy(() -> Plan.create(command, curriculumIds))
				.isInstanceOf(InvalidPlanCurriculumException.class);
	}

	@Test
	void 새_플랜은_완료_시각이_없는_활성_상태로_생성된다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "초코의 훈련 플랜", "기초 훈련을 익혀요");

		Plan plan = Plan.create(command, List.of(7L, 4L));

		assertThat(plan.getUserId()).isEqualTo(1L);
		assertThat(plan.getDogId()).isEqualTo(2L);
		assertThat(plan.getTitle()).isEqualTo(command.title());
		assertThat(plan.getGoal()).isEqualTo(command.goal());
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(plan.getCompletedAt()).isNull();
	}

	@Test
	void 커스텀_플랜은_강아지_이름으로_제목을_짓고_고정된_목표를_가진다() {
		CustomPlanCreateCommand command = new CustomPlanCreateCommand(1L, 2L, "초코");

		Plan plan = Plan.createCustom(command, List.of(7L, 4L));

		assertThat(plan.getUserId()).isEqualTo(1L);
		assertThat(plan.getDogId()).isEqualTo(2L);
		assertThat(plan.getTitle()).isEqualTo("초코의 훈련 플랜");
		assertThat(plan.getGoal()).isEqualTo("선택한 훈련을 차근차근 익혀요");
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(plan.getCurriculumIds()).containsExactly(7L, 4L);
	}

	@Test
	void 커스텀_플랜도_커리큘럼_규칙을_검증한다() {
		assertThatThrownBy(() -> Plan.createCustom(new CustomPlanCreateCommand(1L, 2L, "초코"), List.of()))
				.isInstanceOf(EmptyPlanCurriculumsException.class);
	}

	@Test
	void 전달받은_커리큘럼_순서를_저장하고_입력_목록의_변경에_영향받지_않는다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표");
		List<Long> curriculumIds = new ArrayList<>(List.of(7L, 4L));

		Plan plan = Plan.create(command, curriculumIds);
		curriculumIds.clear();

		assertThat(plan.getCurriculumIds()).containsExactly(7L, 4L);
	}

	@Test
	void 외부에서_플랜의_커리큘럼_목록을_직접_변경할_수_없다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표");
		Plan plan = Plan.create(command, List.of(7L));

		assertThatThrownBy(() -> plan.getCurriculumIds().clear()).isInstanceOf(UnsupportedOperationException.class);
	}
	@Test
	void AI_플랜은_공통_제목과_AI_목표와_사용자_기대사항을_가진다() {
		AiPlanCreateCommand command = new AiPlanCreateCommand(1L, 2L, "초코", "차분히 기다려요",
				"  외출 전에 차분히 기다렸으면 해요  ");
		CustomPlanCreateCommand customCommand = new CustomPlanCreateCommand(1L, 2L, "초코");

		Plan plan = Plan.createAiPlan(command, List.of(7L, 4L));
		Plan customPlan = Plan.createCustom(customCommand, List.of(7L, 4L));

		assertThat(plan.getUserId()).isEqualTo(1L);
		assertThat(plan.getDogId()).isEqualTo(2L);
		assertThat(plan.getTitle()).isEqualTo(customPlan.getTitle());
		assertThat(plan.getGoal()).isEqualTo(command.goal());
		assertThat(plan.getExpectation()).isEqualTo(command.expectation());
		assertThat(customPlan.getExpectation()).isNull();
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(plan.getCurriculumIds()).containsExactly(7L, 4L);
		assertThat(plan.getCompletedLessonIds()).isEmpty();
	}

	@Test
	void AI_플랜도_커리큘럼_규칙을_검증한다() {
		AiPlanCreateCommand valid = new AiPlanCreateCommand(1L, 2L, "초코", "목표", "기대사항");

		assertThatThrownBy(() -> Plan.createAiPlan(valid, List.of()))
				.isInstanceOf(EmptyPlanCurriculumsException.class);
		assertThatThrownBy(() -> Plan.createAiPlan(valid, List.of(7L, 7L)))
				.isInstanceOf(DuplicatePlanCurriculumException.class);
	}

	@Test
	void AI_플랜의_기대사항은_null일_수_없다() {
		AiPlanCreateCommand command = new AiPlanCreateCommand(1L, 2L, "초코", "목표", null);

		assertThatThrownBy(() -> Plan.createAiPlan(command, List.of(7L))).isInstanceOf(NullPointerException.class);
	}

}
