package com.daesabu.meongcoach.analysis.application.provided;

import com.daesabu.meongcoach.analysis.domain.AiAnalysisStatus;
import java.time.LocalDateTime;

public record AiReportDetailResult(Long id, String videoObjectKey, String videoUrl, String title,
		AiAnalysisStatus status, AiReportContent content, LocalDateTime createdAt) {
}
