package com.daesabu.meongcoach.ai.application.provided;

public interface AiContentGenerator {

	/**
	 * 시스템 프롬프트, 사용자 프롬프트와 응답 스키마를 모델에 전달하고 생성된 내용을 문자열로 반환한다.
	 * 모델 호출 실패 또는 사용할 수 없는 응답이면 AiContentGenerationFailedException을 던진다.
	 * 응답 스키마는 JSON 객체 문자열이어야 하며, 잘못된 스키마는 호출 전에 거절한다.
	 */
	String generate(AiContentGenerateRequest request);
}
