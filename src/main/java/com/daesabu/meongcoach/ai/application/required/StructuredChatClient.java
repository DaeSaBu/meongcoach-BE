package com.daesabu.meongcoach.ai.application.required;

import com.daesabu.meongcoach.ai.application.provided.dto.StructuredChatRequest;

public interface StructuredChatClient {

	String complete(StructuredChatRequest request);
}
