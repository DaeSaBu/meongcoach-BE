package com.daesabu.meongcoach.ai.application.provided;

public record AiContentGenerateRequest(String systemPrompt, String userPrompt, String responseSchema) {
}
