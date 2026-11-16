package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.CurriculumProgressFinder;
import com.daesabu.meongcoach.training.application.required.CurriculumProgressRepository;
import com.daesabu.meongcoach.training.domain.CurriculumProgress;
import com.daesabu.meongcoach.training.domain.exception.CurriculumProgressNotFoundException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumProgressQueryService implements CurriculumProgressFinder {

	private final CurriculumProgressRepository curriculumProgressRepository;

	@Override
	@Transactional
	public CurriculumProgress findCurriculumProgressForUpdate(Long userId, Long dogId, Long curriculumId,
			LocalDate trainedDate) {
		return curriculumProgressRepository
				.findLockedByUserIdAndDogIdAndCurriculumIdAndTrainedDate(userId, dogId, curriculumId, trainedDate)
				.orElseThrow(CurriculumProgressNotFoundException::new);
	}
}
