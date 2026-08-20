package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiReport 도메인")
class AiReportTest {

	@Test
	@DisplayName("생성하면 사용자·영상 객체 키·제목·리포트 내용이 설정된다")
	void createSetsUserVideoObjectKeyTitleAndContent() {
		AiReport report = AiReport.create(new AiReportCreateCommand(1L,
				"videos/training/1/video.mp4", "분리불안 징후 행동 분석", "분리불안 징후가 관찰됩니다."));

		assertThat(report.getUserId()).isEqualTo(1L);
		assertThat(report.getVideoObjectKey()).isEqualTo("videos/training/1/video.mp4");
		assertThat(report.getTitle()).isEqualTo("분리불안 징후 행동 분석");
		assertThat(report.getContent()).isEqualTo("분리불안 징후가 관찰됩니다.");
	}

	@Test
	@DisplayName("생성하면 상태가 COMPLETED로 설정된다")
	void createSetsStatusCompleted() {
		// create()는 분석 성공 경로에서만 호출되므로 상태는 항상 COMPLETED다
		AiReport report = AiReport.create(new AiReportCreateCommand(1L,
				"videos/training/1/video.mp4", "분리불안 징후 행동 분석", "분리불안 징후가 관찰됩니다."));

		assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
	}

	@Test
	@DisplayName("제목 없이도 생성할 수 있다")
	void createAllowsNullTitle() {
		AiReport report = AiReport.create(new AiReportCreateCommand(1L,
				"videos/training/1/video.mp4", null, "분리불안 징후가 관찰됩니다."));

		assertThat(report.getTitle()).isNull();
	}

	@Test
	@DisplayName("컬럼 길이를 넘는 제목은 잘라서 저장한다")
	void createTruncatesTitleOverColumnLength() {
		// 제목은 부가 정보라 길이 위반으로 리포트 저장 전체가 실패해서는 안 된다
		AiReport report = AiReport.create(new AiReportCreateCommand(1L,
				"videos/training/1/video.mp4", "가".repeat(500), "분리불안 징후가 관찰됩니다."));

		assertThat(report.getTitle()).hasSize(AiReport.TITLE_MAX_LENGTH);
	}

	@Test
	@DisplayName("공백뿐인 제목은 null로 저장한다")
	void createNormalizesBlankTitleToNull() {
		AiReport report = AiReport.create(new AiReportCreateCommand(1L,
				"videos/training/1/video.mp4", "   ", "분리불안 징후가 관찰됩니다."));

		assertThat(report.getTitle()).isNull();
	}

	@Test
	@DisplayName("제목 앞뒤 공백은 제거해 저장한다")
	void createStripsTitleWhitespace() {
		AiReport report = AiReport.create(new AiReportCreateCommand(1L,
				"videos/training/1/video.mp4", "  분리불안 징후 행동 분석  ", "분리불안 징후가 관찰됩니다."));

		assertThat(report.getTitle()).isEqualTo("분리불안 징후 행동 분석");
	}
}
