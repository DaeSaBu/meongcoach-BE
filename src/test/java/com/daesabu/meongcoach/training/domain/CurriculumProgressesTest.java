package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurriculumProgressesTest {

	private static final LocalDate TRAINED_DATE = LocalDate.of(2026, 11, 16);

	private static final Long CURRICULUM_ID = 10L;

	@Test
	void 그날_그_커리큘럼의_상태를_찾는다() {
		CurriculumProgress succeeded = register(CURRICULUM_ID, TRAINED_DATE);
		succeeded.succeed();
		CurriculumProgress failed = register(CURRICULUM_ID, TRAINED_DATE.plusDays(1));
		failed.fail();
		CurriculumProgresses progresses = new CurriculumProgresses(List.of(succeeded, failed));

		assertThat(progresses.findStatus(CURRICULUM_ID, TRAINED_DATE)).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
		assertThat(progresses.findStatus(CURRICULUM_ID, TRAINED_DATE.plusDays(1)))
				.isEqualTo(CurriculumProgressStatus.FAILED);
	}

	@Test
	void 같은_날_다른_커리큘럼의_상태와_섞이지_않는다() {
		CurriculumProgress succeeded = register(CURRICULUM_ID, TRAINED_DATE);
		succeeded.succeed();
		CurriculumProgress other = register(11L, TRAINED_DATE);
		CurriculumProgresses progresses = new CurriculumProgresses(List.of(succeeded, other));

		assertThat(progresses.findStatus(11L, TRAINED_DATE)).isEqualTo(CurriculumProgressStatus.PENDING);
	}

	@Test
	void 기록이_없는_날의_커리큘럼은_판정을_기다리는_상태다() {
		CurriculumProgresses progresses = new CurriculumProgresses(List.of());

		assertThat(progresses.findStatus(CURRICULUM_ID, TRAINED_DATE)).isEqualTo(CurriculumProgressStatus.PENDING);
	}

	private CurriculumProgress register(Long curriculumId, LocalDate trainedDate) {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(2L, curriculumId,
				trainedDate);
		return CurriculumProgress.register(1L, command);
	}
}
