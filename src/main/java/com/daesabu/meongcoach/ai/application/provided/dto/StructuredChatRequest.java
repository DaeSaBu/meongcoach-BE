package com.daesabu.meongcoach.ai.application.provided.dto;

import java.time.Duration;

public record StructuredChatRequest(String systemPrompt, String userPrompt, String schemaName, String schemaJson,
		Duration timeout) {

	public StructuredChatRequest(String systemPrompt, String userPrompt, String schemaName, String schemaJson) {
		this(systemPrompt, userPrompt, schemaName, schemaJson, null);
	}
}
