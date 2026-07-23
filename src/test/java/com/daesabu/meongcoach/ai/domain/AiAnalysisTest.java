package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiAnalysisTest {

	private AiAnalysis uploadAnalysis() {
		return AiAnalysis.upload(1L, 5L, "https://s3.meongcoach.com/video.mp4", 60, 10_000_000L);
	}

	@Test
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
	void startProcessingChangesStatusToProcessing() {
		AiAnalysis analysis = uploadAnalysis();

		analysis.startProcessing();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.PROCESSING);
	}

	@Test
	void completeChangesStatusToCompleted() {
		AiAnalysis analysis = uploadAnalysis();
		analysis.startProcessing();

		analysis.complete();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.COMPLETED);
	}

	@Test
	void failChangesStatusToFailed() {
		AiAnalysis analysis = uploadAnalysis();
		analysis.startProcessing();

		analysis.fail();

		assertThat(analysis.getStatus()).isEqualTo(AiAnalysisStatus.FAILED);
	}
}
