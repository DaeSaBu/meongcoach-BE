package com.daesabu.meongcoach.ai.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class AiContentGenerationFailedException extends DomainException {

	public AiContentGenerationFailedException(Throwable cause) {
		super(AiErrorCode.AI_CONTENT_GENERATION_FAILED, AiErrorCode.AI_CONTENT_GENERATION_FAILED.message(), cause);
	}
}
