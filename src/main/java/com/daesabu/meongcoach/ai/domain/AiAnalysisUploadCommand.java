package com.daesabu.meongcoach.ai.domain;

import java.time.LocalDateTime;

public record AiAnalysisUploadCommand(Long userId, String videoObjectKey, LocalDateTime uploadExpiresAt) {
}
