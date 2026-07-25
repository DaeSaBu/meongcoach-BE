package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiAnalysis 도메인")
class AiAnalysisTest {

	private AiAnalysis uploadAnalysis() {
		return AiAnalysis.upload(new AiAnalysisUploadCommand(1L, 5L, "https://s3.meongcoach.com/video.mp4", 60,
				10_000_000L));
	}

	@Test
	@DisplayName("업로드하면 상태가 UPLOADED로 초기화된다")
	void uploadInitializesStatusToUploaded() {
		AiAnalysis analysis = uploadAnalysis();

		assertThat(analysis.getUserId()).isEqualTo(1L);
		assertThat(analysis.getDogId()).isEqualTo(5L);
		assertThat(analysis.getVideoUrl()).isEqualTo("https://s3.meongcoach.com/video.mp4");
		assertThat(analysis.getVideoLengthSec()).isEqualTo(60);
		assertThat(analysis.getFileSizeBytes()).isEqualTo(10_000_000L);
		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.UPLOADED);
	}

	@Test
	@DisplayName("처리를 시작하면 상태가 PROCESSING으로 변경된다")
	void startProcessingChangesStatusToProcessing() {
		AiAnalysis analysis = uploadAnalysis();

		analysis.startProcessing();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.PROCESSING);
	}

	@Test
	@DisplayName("완료하면 상태가 COMPLETED로 변경된다")
	void completeChangesStatusToCompleted() {
		AiAnalysis analysis = uploadAnalysis();
		analysis.startProcessing();

		analysis.complete();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
	}

	@Test
	@DisplayName("실패 처리하면 상태가 FAILED로 변경된다")
	void failChangesStatusToFailed() {
		AiAnalysis analysis = uploadAnalysis();
		analysis.startProcessing();

		analysis.fail();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.FAILED);
	}
}
