package com.daesabu.meongcoach.ai.application.provided;

/**
 * AI 리포트용 영상 업로드 URL 발급 능력.
 */
public interface AiVideoUploadUrlIssuer {

	/**
	 * 무료 체험 한도를 검증한 뒤 영상 업로드 URL을 발급한다.
	 * 한도를 소진했으면 {@code AiReportTrialExceededException}을 던진다.
	 */
	AiVideoUploadUrlView issue(Long userId, String contentType, long fileSizeBytes);
}
