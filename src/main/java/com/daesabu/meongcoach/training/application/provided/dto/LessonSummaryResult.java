package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TrainingCategory;

public record LessonSummaryResult(Long id, String title, TrainingContentResult curriculum,
		TrainingContentResult topic, TrainingContentResult category) {

	public static LessonSummaryResult from(Lesson lesson) {
		Curriculum curriculum = lesson.getCurriculum();
		Topic topic = curriculum.getTopic();
		TrainingCategory category = topic.getTrainingCategory();
		return new LessonSummaryResult(lesson.getId(), lesson.getTitle(),
				new TrainingContentResult(curriculum.getId(), curriculum.getTitle()),
				new TrainingContentResult(topic.getId(), topic.getTitle()),
				new TrainingContentResult(category.getId(), category.getTitle()));
	}
}
