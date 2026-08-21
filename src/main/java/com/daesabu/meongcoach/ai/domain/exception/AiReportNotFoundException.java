package com.daesabu.meongcoach.ai.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class AiReportNotFoundException extends DomainException {

	// 남의 리포트를 조회한 경우도 존재 여부를 숨기기 위해 같은 미존재 메시지를 쓴다
	public AiReportNotFoundException(Long reportId) {
		super(AiErrorCode.AI_REPORT_NOT_FOUND, "id가 " + reportId + "인 AI 리포트를 찾을 수 없습니다.");
	}
}
