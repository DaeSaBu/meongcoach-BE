package com.daesabu.meongcoach.ai.application.required;

import com.daesabu.meongcoach.ai.application.required.dto.VideoAnalysisResult;

/**
 * 영상 분석 연동 지점. 구현은 adapter/integration의 어댑터가 담당한다.
 */
public interface VideoAnalyzer {

	/**
	 * 분석 스토리지 URI(gs://버킷/키)의 영상을 분석해 리포트 제목과 recommend/report/solution 구조로
	 * 검증된 리포트 JSON 문자열을 함께 돌려준다. 제목은 모델이 비워 두면 null일 수 있다.
	 * 분석에 실패하거나 응답이 구조에 맞지 않으면
	 * {@link com.daesabu.meongcoach.ai.domain.exception.VideoAnalysisFailedException}을 던진다.
	 */
	VideoAnalysisResult analyze(String videoUri);
}
