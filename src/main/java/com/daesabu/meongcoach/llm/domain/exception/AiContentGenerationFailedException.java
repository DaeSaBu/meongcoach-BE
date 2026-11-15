package com.daesabu.meongcoach.llm.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class AiContentGenerationFailedException extends DomainException {

	public AiContentGenerationFailedException(String detail, Throwable cause) {
		super(LlmErrorCode.AI_CONTENT_GENERATION_FAILED, detail, cause);
	}
}
