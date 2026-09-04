package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class CurriculumNotFoundException extends DomainException {

	public CurriculumNotFoundException(Long curriculumId) {
		super(TrainingErrorCode.TRAINING_CURRICULUM_NOT_FOUND, "id가 " + curriculumId + "인 커리큘럼을 찾을 수 없습니다.");
	}
}
