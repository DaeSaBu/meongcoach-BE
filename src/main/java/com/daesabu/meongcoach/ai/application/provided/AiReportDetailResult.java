package com.daesabu.meongcoach.ai.application.provided;

import com.daesabu.meongcoach.ai.domain.AiAnalysisStatus;
import java.time.LocalDateTime;

public record AiReportDetailResult(Long id, String videoObjectKey, String videoUrl, String title,
		AiAnalysisStatus status, AiReportContent content, LocalDateTime createdAt) {
}
