package com.daesabu.meongcoach.plan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.plan.domain.exception.DuplicatePlanCurriculumException;
import com.daesabu.meongcoach.plan.domain.exception.EmptyPlanCurriculumsException;
import com.daesabu.meongcoach.plan.domain.exception.InvalidDailyMinutesException;
import com.daesabu.meongcoach.plan.domain.exception.InvalidDaysPerWeekException;
import com.daesabu.meongcoach.plan.domain.exception.InvalidPlanCurriculumException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PlanTest {

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
	void 전달받은_교육_순서에서_첫_미완료_레슨을_찾는다() {
		Plan plan = createPlan();
		List<Long> orderedLessonIds = List.of(103L, 101L, 102L);

		assertThat(plan.findCurrentLessonId(orderedLessonIds)).contains(103L);
		plan.completeLesson(103L);
		assertThat(plan.findCurrentLessonId(orderedLessonIds)).contains(101L);
		plan.completeLesson(101L);
		assertThat(plan.findCurrentLessonId(orderedLessonIds)).contains(102L);
		plan.completeLesson(102L);
		assertThat(plan.findCurrentLessonId(orderedLessonIds)).isEmpty();
	}

	@Test
	void 교육_목록_앞에_새_레슨이_추가되면_그_레슨부터_진행한다() {
		Plan plan = createPlan();
		plan.completeLesson(101L);
		plan.completeLesson(102L);

		assertThat(plan.findCurrentLessonId(List.of(104L, 102L, 103L))).contains(104L);
		plan.completeLesson(104L);
		assertThat(plan.findCurrentLessonId(List.of(104L, 102L, 103L))).contains(103L);
	}

	@Test
	void 레슨_목록이_비어_있으면_현재_레슨_탐색을_거절한다() {
		Plan plan = createPlan();

		assertThatThrownBy(() -> plan.findCurrentLessonId(List.of()))
				.isInstanceOf(IllegalStateException.class);
	}

	private Plan createPlan() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", 10, 3);
		return Plan.create(command, List.of(7L, 4L));
	}

	@Test
	void 커리큘럼_목록이_비어_있으면_생성을_거절한다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", 10, 3);

		assertThatThrownBy(() -> Plan.create(command, List.of()))
				.isInstanceOf(EmptyPlanCurriculumsException.class);
	}

	@Test
	void 중복_커리큘럼이_있으면_제거하지_않고_생성을_거절한다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", 10, 3);
		List<Long> curriculumIds = new ArrayList<>(List.of(7L, 4L, 7L));

		assertThatThrownBy(() -> Plan.create(command, curriculumIds))
				.isInstanceOf(DuplicatePlanCurriculumException.class);
		assertThat(curriculumIds).containsExactly(7L, 4L, 7L);
	}

	@Test
	void 커리큘럼_ID에_null이_있으면_생성을_거절한다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", 10, 3);
		List<Long> curriculumIds = Arrays.asList(7L, null);

		assertThatThrownBy(() -> Plan.create(command, curriculumIds))
				.isInstanceOf(InvalidPlanCurriculumException.class);
	}

	@ParameterizedTest
	@CsvSource({"5, 3", "5, 5", "5, 7", "10, 3", "10, 5", "10, 7", "20, 3", "20, 5", "20, 7"})
	void 허용된_실천_약속으로_플랜을_생성한다(int dailyMinutes, int daysPerWeek) {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", dailyMinutes, daysPerWeek);

		Plan plan = Plan.create(command, List.of(7L));

		assertThat(plan.getDailyMinutes()).isEqualTo(dailyMinutes);
		assertThat(plan.getDaysPerWeek()).isEqualTo(daysPerWeek);
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 0, 1, 15, 21, 30})
	void 선택지에_없는_하루_훈련_시간은_거절한다(int dailyMinutes) {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", dailyMinutes, 3);

		assertThatThrownBy(() -> Plan.create(command, List.of(7L)))
				.isInstanceOf(InvalidDailyMinutesException.class);
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 0, 1, 2, 4, 6, 8})
	void 선택지에_없는_주당_훈련_일수는_거절한다(int daysPerWeek) {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", 10, daysPerWeek);

		assertThatThrownBy(() -> Plan.create(command, List.of(7L)))
				.isInstanceOf(InvalidDaysPerWeekException.class);
	}

	@Test
	void 새_플랜은_완료_시각이_없는_활성_상태로_생성된다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "초코의 2주 훈련 플랜", "기초 훈련을 익혀요", 10, 3);

		Plan plan = Plan.create(command, List.of(7L, 4L));

		assertThat(plan.getUserId()).isEqualTo(1L);
		assertThat(plan.getDogId()).isEqualTo(2L);
		assertThat(plan.getTitle()).isEqualTo(command.title());
		assertThat(plan.getGoal()).isEqualTo(command.goal());
		assertThat(plan.getDailyMinutes()).isEqualTo(10);
		assertThat(plan.getDaysPerWeek()).isEqualTo(3);
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(plan.getCompletedAt()).isNull();
	}

	@Test
	void 전달받은_커리큘럼_순서를_저장하고_입력_목록의_변경에_영향받지_않는다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", 10, 3);
		List<Long> curriculumIds = new ArrayList<>(List.of(7L, 4L));

		Plan plan = Plan.create(command, curriculumIds);
		curriculumIds.clear();

		assertThat(plan.getCurriculumIds()).containsExactly(7L, 4L);
	}

	@Test
	void 외부에서_플랜의_커리큘럼_목록을_직접_변경할_수_없다() {
		PlanCreateCommand command = new PlanCreateCommand(1L, 2L, "제목", "목표", 10, 3);
		Plan plan = Plan.create(command, List.of(7L));

		assertThatThrownBy(() -> plan.getCurriculumIds().clear()).isInstanceOf(UnsupportedOperationException.class);
	}
}
