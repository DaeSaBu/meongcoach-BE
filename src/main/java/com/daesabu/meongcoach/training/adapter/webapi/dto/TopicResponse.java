package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.domain.Topic;

/**
 * 토픽 응답.
 */
public record TopicResponse(
		Long topicId,
		String topicTitle,
		String topicDescription,
		String topicDetail,
		String topicIconUrl,
		int topicSortOrder
) {

	public static TopicResponse from(Topic topic) {
		return new TopicResponse(
				topic.getId(),
				topic.getTitle(),
				topic.getDescription(),
				topic.getDetail(),
				topic.getIconUrl(),
				topic.getSortOrder()
		);
	}
}
