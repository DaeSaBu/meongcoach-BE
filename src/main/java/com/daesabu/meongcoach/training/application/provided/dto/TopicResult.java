package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Topic;

public record TopicResult(Long id, String title, String description) {

	public static TopicResult from(Topic topic) {
		return new TopicResult(topic.getId(), topic.getTitle(), topic.getDescription());
	}
}
