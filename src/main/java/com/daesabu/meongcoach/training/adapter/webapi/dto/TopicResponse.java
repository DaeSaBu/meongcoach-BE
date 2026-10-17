package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.domain.Topic;

public record TopicResponse(
		Long topicId,
		String topicTitle,
		String topicDescription,
		String topicDetail,
		String topicIconUrl
) {

	public static TopicResponse from(Topic topic) {
		return new TopicResponse(
				topic.getId(),
				topic.getTitle(),
				topic.getDescription(),
				topic.getDetail(),
				topic.getIconUrl()
		);
	}
}
