package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.TopicResult;

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

	public static TopicResponse from(TopicResult result) {
		return new TopicResponse(
				result.id(),
				result.title(),
				result.description(),
				result.detail(),
				result.iconUrl(),
				result.sortOrder()
		);
	}
}
