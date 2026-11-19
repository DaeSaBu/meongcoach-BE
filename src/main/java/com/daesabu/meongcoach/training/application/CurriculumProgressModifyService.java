package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumProgressFinder;
import com.daesabu.meongcoach.training.application.provided.CurriculumProgressRegister;
import com.daesabu.meongcoach.training.application.provided.dto.CurriculumProgressResultRequest;
import com.daesabu.meongcoach.training.domain.CurriculumProgress;
import com.daesabu.meongcoach.training.domain.CurriculumProgressStatus;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumProgressModifyService implements CurriculumProgressRegister {

	private final DogProfileFinder dogProfileFinder;
	private final CurriculumProgressFinder curriculumProgressFinder;

	@Override
	@Transactional
	public void recordResult(Long userId, Long dogId, Long curriculumId, LocalDate trainedDate,
			CurriculumProgressResultRequest request) {
		dogProfileFinder.findDogProfile(userId, dogId);
		CurriculumProgressStatus result = CurriculumProgressStatus.fromResult(request.result());
		CurriculumProgress curriculumProgress = curriculumProgressFinder.findCurriculumProgressForUpdate(userId,
				dogId, curriculumId, trainedDate);
		if (curriculumProgress.getStatus() == result) {
			return;
		}
		if (result == CurriculumProgressStatus.SUCCEEDED) {
			curriculumProgress.succeed();
			return;
		}
		curriculumProgress.fail();
	}
}
