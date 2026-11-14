package com.daesabu.meongcoach.llm.application.provided;

public record AiContentGenerateRequest(String systemPrompt, String userPrompt, String responseSchema) {
}
