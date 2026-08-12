package com.daesabu.meongcoach.ai.adapter.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * EvoLink 채팅 API(OpenAI 호환) 요청 본문. 소비하는 필드만 담고, 쓰지 않는 선택 필드는 NON_NULL로 생략한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvoLinkChatRequest(
		String model,
		List<ChatMessage> messages,
		@JsonProperty("response_format") ResponseFormat responseFormat,
		@JsonProperty("max_tokens") Integer maxTokens,
		Double temperature,
		Thinking thinking) {

	/**
	 * content는 순수 텍스트면 String, 멀티모달이면 List&lt;ContentPart&gt;를 담는다.
	 * OpenAI 호환 API가 두 형태를 모두 받으므로 Object로 둔다.
	 */
	public record ChatMessage(String role, Object content) {

		public static ChatMessage system(String text) {
			return new ChatMessage("system", text);
		}

		public static ChatMessage user(String text) {
			return new ChatMessage("user", text);
		}

		public static ChatMessage user(List<ContentPart> parts) {
			return new ChatMessage("user", parts);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ContentPart(String type, String text, @JsonProperty("video_url") VideoUrl videoUrl) {

		public static ContentPart text(String text) {
			return new ContentPart("text", text, null);
		}

		public static ContentPart videoUrl(String url, Double fps) {
			return new ContentPart("video_url", null, new VideoUrl(url, fps));
		}
	}

	public record VideoUrl(String url, Double fps) {
	}

	public record ResponseFormat(String type) {

		public static ResponseFormat jsonObject() {
			return new ResponseFormat("json_object");
		}
	}

	public record Thinking(String type) {
	}
}
