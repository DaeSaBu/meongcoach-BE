package com.daesabu.meongcoach.analysis.domain;

import java.time.Instant;

public record AiAnalysisUploadCommand(Long userId, Long dogId, String videoObjectKey, Instant uploadExpiresAt,
                                      String context) {
}
