package com.daesabu.meongcoach.ai.application.required;

/**
 * 리포트 제목 생성 연동 지점. 구현은 adapter/integration의 어댑터가 담당한다.
 */
public interface ReportTitleGenerator {

	/**
	 * 리포트 JSON 본문을 요약하는 한 줄 제목을 돌려준다. 생성에 실패하면
	 * {@link com.daesabu.meongcoach.ai.domain.exception.ReportTitleGenerationFailedException}을 던진다.
	 */
	String generateTitle(String reportContentJson);
}
