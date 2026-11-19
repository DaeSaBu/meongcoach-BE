package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.LessonProgresses;
import com.daesabu.meongcoach.training.domain.Topic;
import java.util.List;

public record CurriculumListResult(Topic topic, List<CurriculumResult> curriculums) {

	public static CurriculumListResult of(Topic topic, LessonProgresses progresses) {
		List<CurriculumResult> curriculums = topic.getCurriculums().stream()
				.map(curriculum -> CurriculumResult.of(curriculum, progresses))
				.toList();
		return new CurriculumListResult(topic, curriculums);
	}
}
