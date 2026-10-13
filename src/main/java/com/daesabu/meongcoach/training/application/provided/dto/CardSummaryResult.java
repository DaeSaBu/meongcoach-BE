package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TrainingCategory;

public record CardSummaryResult(
		Long trainingCategoryId,
		String trainingCategoryTitle,
		Long topicId,
		String topicTitle,
		Long curriculumId,
		String curriculumTitle,
		Long lessonId,
		String lessonTitle,
		Long id,
		String title
) {

	public static CardSummaryResult from(Card card) {
		Lesson lesson = card.getLesson();
		Curriculum curriculum = lesson.getCurriculum();
		Topic topic = curriculum.getTopic();
		TrainingCategory trainingCategory = topic.getTrainingCategory();
		return new CardSummaryResult(
				trainingCategory.getId(), trainingCategory.getTitle(),
				topic.getId(), topic.getTitle(),
				curriculum.getId(), curriculum.getTitle(),
				lesson.getId(), lesson.getTitle(),
				card.getId(), card.getTitle());
	}
}
