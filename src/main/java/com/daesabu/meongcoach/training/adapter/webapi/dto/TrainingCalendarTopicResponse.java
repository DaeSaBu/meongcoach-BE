package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.dto.TrainingCalendarTopicResult;
import com.daesabu.meongcoach.training.domain.Topic;
import java.util.List;

public record TrainingCalendarTopicResponse(Long topicId, String topicTitle, String topicIconUrl,
		List<TrainingCalendarCurriculumResponse> curriculums) {

	public static TrainingCalendarTopicResponse from(TrainingCalendarTopicResult result) {
		Topic topic = result.topic();
		List<TrainingCalendarCurriculumResponse> curriculums = result.curriculums().stream()
				.map(TrainingCalendarCurriculumResponse::from)
				.toList();
		return new TrainingCalendarTopicResponse(topic.getId(), topic.getTitle(), topic.getIconUrl(), curriculums);
	}
}
