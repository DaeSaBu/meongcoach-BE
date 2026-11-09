package com.daesabu.meongcoach.ai.domain;

import java.time.Instant;

public record AiAnalysisUploadCommand(Long userId, String videoObjectKey, Instant uploadExpiresAt, String context) {
}
