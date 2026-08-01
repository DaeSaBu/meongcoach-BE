package com.daesabu.meongcoach.ai.domain;

public record AiReportCreateCommand(Long userId, String videoUrl, String content) {
}
