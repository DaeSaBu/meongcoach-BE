package com.daesabu.meongcoach.ai.application.provided;

public interface AiContentGenerator {

	/**
	 * 시스템 프롬프트, 사용자 프롬프트와 응답 스키마를 모델에 전달하고 생성된 내용을 문자열로 반환한다.
	 */
	String generate(AiContentGenerateRequest request);
}
