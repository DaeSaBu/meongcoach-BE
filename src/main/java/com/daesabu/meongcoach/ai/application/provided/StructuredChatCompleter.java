package com.daesabu.meongcoach.ai.application.provided;

import com.daesabu.meongcoach.ai.application.provided.dto.StructuredChatRequest;

/**
 * 스키마로 형태를 강제한 JSON 응답을 모델에서 받아오는 능력.
 */
public interface StructuredChatCompleter {

	/**
	 * system·user 프롬프트와 JSON 스키마(strict)로 모델을 호출해 스키마를 따르는 JSON 문자열을 돌려준다.
	 * 모델 호출·응답 해석에 실패하면 StructuredChatCompletionFailedException을 던진다.
	 */
	String complete(StructuredChatRequest request);
}
