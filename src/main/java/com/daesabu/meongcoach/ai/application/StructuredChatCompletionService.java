package com.daesabu.meongcoach.ai.application;

import com.daesabu.meongcoach.ai.application.provided.StructuredChatCompleter;
import com.daesabu.meongcoach.ai.application.provided.dto.StructuredChatRequest;
import com.daesabu.meongcoach.ai.application.required.StructuredChatClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StructuredChatCompletionService implements StructuredChatCompleter {

	private final StructuredChatClient structuredChatClient;

	@Override
	public String complete(StructuredChatRequest request) {
		return structuredChatClient.complete(request);
	}
}
