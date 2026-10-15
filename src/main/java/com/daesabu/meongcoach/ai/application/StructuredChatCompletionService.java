package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.StructuredChatCompleter;
import com.daesabu.meongcoach.ai.application.provided.dto.StructuredChatRequest;
import com.daesabu.meongcoach.ai.application.required.StructuredChatClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 구조화 JSON 채팅 서비스. 외부 모델 호출 구간이라 트랜잭션을 걸지 않고 포트에 위임만 한다.
 */
@Service
@RequiredArgsConstructor
public class StructuredChatCompletionService implements StructuredChatCompleter {

	private final StructuredChatClient structuredChatClient;

	@Override
	public String complete(StructuredChatRequest request) {
		return structuredChatClient.complete(request);
	}
}
