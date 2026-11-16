package com.daesabu.meongcoach.training.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.required.CurriculumProgressRepository;
import com.daesabu.meongcoach.training.domain.CurriculumProgress;
import com.daesabu.meongcoach.training.domain.CurriculumProgressRegisterCommand;
import com.daesabu.meongcoach.training.domain.exception.CurriculumProgressNotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CurriculumProgressFinderTest {

	private static final Long USER_ID = 1L;

	private static final Long DOG_ID = 2L;

	private static final Long CURRICULUM_ID = 10L;

	private static final LocalDate TRAINED_DATE = LocalDate.of(2026, 11, 16);

	@Autowired
	private CurriculumProgressFinder curriculumProgressFinder;

	@Autowired
	private CurriculumProgressRepository curriculumProgressRepository;

	@Test
	void 강아지가_그날_훈련한_커리큘럼의_진행_기록을_조회한다() {
		CurriculumProgress saved = saveProgress(DOG_ID, TRAINED_DATE);

		CurriculumProgress found = curriculumProgressFinder.findCurriculumProgressForUpdate(USER_ID, DOG_ID,
				CURRICULUM_ID, TRAINED_DATE);

		assertThat(found.getId()).isEqualTo(saved.getId());
	}

	@Test
	void 다른_날의_기록만_있으면_예외를_던진다() {
		saveProgress(DOG_ID, TRAINED_DATE.minusDays(1));

		assertThatThrownBy(() -> curriculumProgressFinder.findCurriculumProgressForUpdate(USER_ID, DOG_ID,
				CURRICULUM_ID, TRAINED_DATE))
				.isInstanceOf(CurriculumProgressNotFoundException.class);
	}

	@Test
	void 다른_강아지의_기록만_있으면_예외를_던진다() {
		saveProgress(3L, TRAINED_DATE);

		assertThatThrownBy(() -> curriculumProgressFinder.findCurriculumProgressForUpdate(USER_ID, DOG_ID,
				CURRICULUM_ID, TRAINED_DATE))
				.isInstanceOf(CurriculumProgressNotFoundException.class);
	}

	private CurriculumProgress saveProgress(Long dogId, LocalDate trainedDate) {
		CurriculumProgressRegisterCommand command = new CurriculumProgressRegisterCommand(dogId, CURRICULUM_ID,
				trainedDate);
		return curriculumProgressRepository.saveAndFlush(CurriculumProgress.register(USER_ID, command));
	}
}
