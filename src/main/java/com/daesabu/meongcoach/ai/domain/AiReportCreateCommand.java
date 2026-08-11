package com.daesabu.meongcoach.ai.domain;

public record AiReportCreateCommand(Long userId, String videoObjectKey, String title, String content) {
}
