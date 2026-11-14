package com.daesabu.meongcoach.analysis.application.provided;

import com.daesabu.meongcoach.analysis.domain.AiAnalysisStatus;
import java.time.LocalDateTime;

public record AiReportResult(Long id, String videoObjectKey, String title, AiAnalysisStatus status,
		LocalDateTime createdAt) {
}
