package com.daesabu.meongcoach.home.adapter.webapi.dto;

import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import java.util.List;

public record QuickTopicsResponse(List<QuickTopicResponse> topics) {

	public static QuickTopicsResponse from(List<QuickTopicResult> results) {
		List<QuickTopicResponse> topics = results.stream().map(QuickTopicResponse::from).toList();
		return new QuickTopicsResponse(topics);
	}
}
