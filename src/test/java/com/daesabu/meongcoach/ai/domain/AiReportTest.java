package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiReport 도메인")
class AiReportTest {

	@Test
	@DisplayName("생성하면 사용자·영상 URL·리포트 내용이 설정된다")
	void createSetsUserVideoUrlAndContent() {
		AiReport report = AiReport.create(new AiReportCreateCommand(1L,
				"https://s3.meongcoach.com/videos/training/1/video.mp4", "분리불안 징후가 관찰됩니다."));

		assertThat(report.getUserId()).isEqualTo(1L);
		assertThat(report.getVideoUrl()).isEqualTo("https://s3.meongcoach.com/videos/training/1/video.mp4");
		assertThat(report.getContent()).isEqualTo("분리불안 징후가 관찰됩니다.");
	}
}
