package com.daesabu.meongcoach.ai.domain.shared;

import com.daesabu.meongcoach.ai.domain.exception.AiErrorCode;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class StructuredChatCompletionFailedException extends DomainException {

	public StructuredChatCompletionFailedException(String detail) {
		super(AiErrorCode.AI_STRUCTURED_CHAT_FAILED, detail);
	}

	public StructuredChatCompletionFailedException(String detail, Throwable cause) {
		super(AiErrorCode.AI_STRUCTURED_CHAT_FAILED, detail, cause);
	}
}
