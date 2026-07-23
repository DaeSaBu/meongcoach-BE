package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiReportTest {

	@Test
	void createSetsAnalysisAndContent() {
		AiAnalysis analysis = AiAnalysis.upload(1L, null, "https://s3.meongcoach.com/video.mp4", 60, 10_000_000L);

		AiReport report = AiReport.create(analysis, "분리불안 징후가 관찰됩니다.", AiReportStatus.PUBLISHED);

		assertThat(report.getAnalysis()).isEqualTo(analysis);
		assertThat(report.getContent()).isEqualTo("분리불안 징후가 관찰됩니다.");
		assertThat(report.getStatus()).isEqualTo(AiReportStatus.PUBLISHED);
	}
}
