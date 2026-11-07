package com.daesabu.meongcoach.ai.application.provided;

import com.daesabu.meongcoach.ai.domain.AiAnalysisStatus;
import java.time.LocalDateTime;

public record AiReportResult(Long id, String videoObjectKey, String title, AiAnalysisStatus status,
		LocalDateTime createdAt) {
}
