package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.training.domain.Topic;

public record TopicsResult(Long id, String title, String description) {

	public static TopicsResult from(Topic topic) {
		return new TopicsResult(topic.getId(), topic.getTitle(), topic.getDescription());
	}
}
