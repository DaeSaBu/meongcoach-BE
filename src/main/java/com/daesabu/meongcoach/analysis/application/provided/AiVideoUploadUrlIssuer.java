package com.daesabu.meongcoach.analysis.application.provided;

import com.daesabu.meongcoach.analysis.application.provided.dto.AiVideoUploadUrlRequest;
import jakarta.validation.Valid;

/**
 * AI 리포트용 영상 업로드 URL 발급 능력.
 */
public interface AiVideoUploadUrlIssuer {

	/**
	 * 무료 체험 한도와 영상 길이를 검증한 뒤 사용자가 선택한 강아지의 분석 건을 저장하고 영상 업로드 URL을 발급한다.
	 * 한도를 소진했으면 {@code AiReportTrialExceededException}을,
	 * 길이가 10초 이상 300초 이하가 아니면 {@code AiVideoDurationOutOfRangeException}을,
	 * 선택된 강아지가 없으면 {@code DogNotFoundException}을 던진다.
	 */
	AiVideoUploadUrlResult issue(Long userId, @Valid AiVideoUploadUrlRequest request);
}
