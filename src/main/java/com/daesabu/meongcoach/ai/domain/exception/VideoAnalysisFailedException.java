package com.daesabu.meongcoach.ai.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 영상 분석 연동(모델 호출·응답 해석)이 실패한 경우. HTTP 응답으로 나가는 일은 없지만(컨슈머가 삼킨다),
 * 외부 연동 어댑터가 인프라 예외를 경계에서 도메인 예외로 번역하는 컨벤션에 따라 둔다.
 */
public class VideoAnalysisFailedException extends DomainException {

	public VideoAnalysisFailedException(String detail) {
		super(AiErrorCode.AI_VIDEO_ANALYSIS_FAILED, detail);
	}

	public VideoAnalysisFailedException(String detail, Throwable cause) {
		super(AiErrorCode.AI_VIDEO_ANALYSIS_FAILED, detail, cause);
	}
}
