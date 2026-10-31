package com.daesabu.meongcoach.ai.adapter.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvoLinkChatRequest(
		String model,
		List<ChatMessage> messages,
		@JsonProperty("response_format") ResponseFormat responseFormat,
		@JsonProperty("max_tokens") Integer maxTokens,
		Double temperature,
		Thinking thinking) {

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

	public record ContentPart(String type, String text) {

		public static ContentPart text(String text) {
			return new ContentPart("text", text);
		}
	}

	public record ResponseFormat(String type, @JsonProperty("json_schema") JsonSchema jsonSchema) {
		public static ResponseFormat jsonSchema(String name, Object schema) {
			return new ResponseFormat("json_schema", new JsonSchema(name, schema, true));
		}
	}

	public record JsonSchema(String name, Object schema, Boolean strict) {
	}

	public record Thinking(String type) {
	}
}
