package com.daesabu.meongcoach.analysis.domain;

import java.time.Instant;

public record AiAnalysisUploadCommand(Long userId, String videoObjectKey, Instant uploadExpiresAt, String context) {
}
