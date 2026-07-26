package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiReport 도메인")
class AiReportTest {

	@Test
	@DisplayName("생성하면 분석과 리포트 내용이 설정된다")
	void createSetsAnalysisAndContent() {
		AiAnalysis analysis = AiAnalysis.upload(new AiAnalysisUploadCommand(1L, null,
				"https://s3.meongcoach.com/video.mp4", 60, 10_000_000L));

		AiReport report = AiReport.create(analysis,
				new AiReportCreateCommand("분리불안 징후가 관찰됩니다.", AiReportStatus.PUBLISHED));

		assertThat(report.getAnalysis()).isEqualTo(analysis);
		assertThat(report.getContent()).isEqualTo("분리불안 징후가 관찰됩니다.");
		assertThat(report.getStatus()).isEqualTo(AiReportStatus.PUBLISHED);
	}
}
