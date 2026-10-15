package com.daesabu.meongcoach.ai.application.provided.dto;

public record StructuredChatRequest(String systemPrompt, String userPrompt, String schemaName, String schemaJson) {
}
