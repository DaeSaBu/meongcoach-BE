package com.daesabu.meongcoach.plan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.plan.domain.exception.DuplicatePlanCurriculumException;
import com.daesabu.meongcoach.plan.domain.exception.EmptyPlanCurriculumsException;
import com.daesabu.meongcoach.plan.domain.exception.InvalidDailyMinutesException;
import com.daesabu.meongcoach.plan.domain.exception.InvalidDaysPerWeekException;
import com.daesabu.meongcoach.plan.domain.exception.InvalidPlanCurriculumException;
import java.time.LocalDateTime;
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
		LocalDateTime completedAt = LocalDateTime.of(2026, 10, 23, 12, 0);

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
		LocalDateTime completedAt = LocalDateTime.of(2026, 10, 23, 12, 0);
		plan.complete(completedAt);

		assertThatThrownBy(() -> plan.complete(completedAt.plusDays(1)))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(plan::replace).isInstanceOf(IllegalStateException.class);
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(plan.getCompletedAt()).isEqualTo(completedAt);
	}

	@Test
	void 교체된_플랜은_완료하거나_다시_교체할_수_없다() {
		Plan plan = createPlan();
		plan.replace();
		LocalDateTime completedAt = LocalDateTime.of(2026, 10, 23, 12, 0);

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
	void 커스텀_플랜은_강아지_이름으로_제목을_짓고_고정된_목표를_가진다() {
		CustomPlanCreateCommand command = new CustomPlanCreateCommand(1L, 2L, "초코", 10, 3);

		Plan plan = Plan.createCustom(command, List.of(7L, 4L));

		assertThat(plan.getUserId()).isEqualTo(1L);
		assertThat(plan.getDogId()).isEqualTo(2L);
		assertThat(plan.getTitle()).isEqualTo("초코의 2주 훈련 플랜");
		assertThat(plan.getGoal()).isEqualTo("선택한 훈련을 차근차근 익혀요");
		assertThat(plan.getDailyMinutes()).isEqualTo(10);
		assertThat(plan.getDaysPerWeek()).isEqualTo(3);
		assertThat(plan.getStatus()).isEqualTo(PlanStatus.ACTIVE);
		assertThat(plan.getCurriculumIds()).containsExactly(7L, 4L);
	}

	@Test
	void 커스텀_플랜도_실천_약속과_커리큘럼_규칙을_검증한다() {
		CustomPlanCreateCommand command = new CustomPlanCreateCommand(1L, 2L, "초코", 15, 3);

		assertThatThrownBy(() -> Plan.createCustom(command, List.of(7L)))
				.isInstanceOf(InvalidDailyMinutesException.class);
		assertThatThrownBy(() -> Plan.createCustom(new CustomPlanCreateCommand(1L, 2L, "초코", 10, 3), List.of()))
				.isInstanceOf(EmptyPlanCurriculumsException.class);
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
