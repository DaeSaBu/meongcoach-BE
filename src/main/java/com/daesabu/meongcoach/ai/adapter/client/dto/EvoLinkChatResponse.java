package com.daesabu.meongcoach.ai.adapter.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * EvoLink 채팅 API(OpenAI 호환) 응답 중 소비에 필요한 부분만 담는다.
 * usage·reasoning_content 등 나머지 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvoLinkChatResponse(List<Choice> choices) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Choice(ChatMessage message, @JsonProperty("finish_reason") String finishReason) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ChatMessage(String content) {
	}
}
