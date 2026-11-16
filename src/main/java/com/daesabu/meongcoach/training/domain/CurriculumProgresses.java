package com.daesabu.meongcoach.training.domain;

import java.time.LocalDate;
import java.util.List;

public class CurriculumProgresses {

	private final List<CurriculumProgress> curriculumProgresses;

	public CurriculumProgresses(List<CurriculumProgress> curriculumProgresses) {
		this.curriculumProgresses = List.copyOf(curriculumProgresses);
	}

	public CurriculumProgressStatus findStatus(Long curriculumId, LocalDate trainedDate) {
		return curriculumProgresses.stream()
				.filter(curriculumProgress -> curriculumProgress.getCurriculumId().equals(curriculumId))
				.filter(curriculumProgress -> curriculumProgress.getTrainedDate().equals(trainedDate))
				.findFirst()
				.map(CurriculumProgress::getStatus)
				.orElse(CurriculumProgressStatus.PENDING);
	}
}
