package com.daesabu.meongcoach.ai.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class AiContentGenerationFailedException extends DomainException {

	public AiContentGenerationFailedException(String detail, Throwable cause) {
		super(AiErrorCode.AI_CONTENT_GENERATION_FAILED, detail, cause);
	}
}
