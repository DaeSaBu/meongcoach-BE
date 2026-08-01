package com.daesabu.meongcoach.training.adapter.webapi.dto;

import com.daesabu.meongcoach.training.application.provided.TopicView;

/**
 * 토픽 응답.
 */
public record TopicResponse(Long topicId, String topicTitle, int topicSortOrder) {

	public static TopicResponse from(TopicView view) {
		return new TopicResponse(view.id(), view.title(), view.sortOrder());
	}
}
