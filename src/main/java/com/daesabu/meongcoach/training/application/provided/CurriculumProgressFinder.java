package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.CurriculumProgress;
import java.time.LocalDate;

public interface CurriculumProgressFinder {

	/**
	 * 강아지가 그날 훈련한 커리큘럼의 진행 기록을 배타 잠금으로 조회한다.
	 * 기록이 없으면 {@code CurriculumProgressNotFoundException}을 던진다.
	 */
	CurriculumProgress findCurriculumProgressForUpdate(Long userId, Long dogId, Long curriculumId,
			LocalDate trainedDate);
}
