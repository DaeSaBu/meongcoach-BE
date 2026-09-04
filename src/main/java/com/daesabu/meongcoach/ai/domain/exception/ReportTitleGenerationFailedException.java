package com.daesabu.meongcoach.ai.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 리포트 제목 생성 연동(모델 호출·응답 해석)이 실패한 경우. HTTP 응답으로 나가는 일은 없지만(호출부가 제목 없이 진행한다),
 * 외부 연동 어댑터가 인프라 예외를 경계에서 도메인 예외로 번역하는 컨벤션에 따라 둔다.
 */
public class ReportTitleGenerationFailedException extends DomainException {

	public ReportTitleGenerationFailedException(String detail) {
		super(AiErrorCode.AI_REPORT_TITLE_GENERATION_FAILED, detail);
	}

	public ReportTitleGenerationFailedException(String detail, Throwable cause) {
		super(AiErrorCode.AI_REPORT_TITLE_GENERATION_FAILED, detail, cause);
	}
}
