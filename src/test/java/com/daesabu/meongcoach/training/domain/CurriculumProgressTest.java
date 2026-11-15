package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CurriculumProgressTest {

	private static final LocalDate TRAINED_DATE = LocalDate.of(2026, 11, 16);

	@Test
	void 등록하면_대상과_날짜가_담기고_판정을_기다리는_상태가_된다() {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(1L, 2L, 10L, TRAINED_DATE);

		CurriculumProgress progress = CurriculumProgress.register(command);

		assertThat(progress.getUserId()).isEqualTo(1L);
		assertThat(progress.getDogId()).isEqualTo(2L);
		assertThat(progress.getCurriculumId()).isEqualTo(10L);
		assertThat(progress.getTrainedDate()).isEqualTo(TRAINED_DATE);
		assertThat(progress.getStatus()).isEqualTo(CurriculumProgressStatus.PENDING);
	}

	@Test
	void 판정을_기다리는_날을_성공으로_바꾼다() {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(1L, 2L, 10L, TRAINED_DATE);
		CurriculumProgress progress = CurriculumProgress.register(command);

		progress.succeed();

		assertThat(progress.getStatus()).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
	}

	@Test
	void 이미_성공한_날은_성공으로_남는다() {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(1L, 2L, 10L, TRAINED_DATE);
		CurriculumProgress progress = CurriculumProgress.register(command);
		progress.succeed();

		progress.succeed();

		assertThat(progress.getStatus()).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
	}

	@Test
	void 판정을_기다리는_날을_실패로_바꾼다() {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(1L, 2L, 10L, TRAINED_DATE);
		CurriculumProgress progress = CurriculumProgress.register(command);

		progress.fail();

		assertThat(progress.getStatus()).isEqualTo(CurriculumProgressStatus.FAILED);
	}

	@Test
	void 실패한_날을_성공으로_바꾼다() {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(1L, 2L, 10L, TRAINED_DATE);
		CurriculumProgress progress = CurriculumProgress.register(command);
		progress.fail();

		progress.succeed();

		assertThat(progress.getStatus()).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
	}

	@Test
	void 성공한_날은_실패로_바꿀_수_없다() {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(1L, 2L, 10L, TRAINED_DATE);
		CurriculumProgress progress = CurriculumProgress.register(command);
		progress.succeed();

		assertThatThrownBy(progress::fail)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("성공한 날은 실패로 바꿀 수 없습니다.");
		assertThat(progress.getStatus()).isEqualTo(CurriculumProgressStatus.SUCCEEDED);
	}
}
