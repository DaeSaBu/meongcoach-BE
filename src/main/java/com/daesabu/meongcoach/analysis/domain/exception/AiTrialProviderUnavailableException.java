package com.daesabu.meongcoach.analysis.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class AiTrialProviderUnavailableException extends DomainException {

	public AiTrialProviderUnavailableException() {
		super(AiErrorCode.AI_TRIAL_PROVIDER_UNAVAILABLE);
	}

	public AiTrialProviderUnavailableException(Throwable cause) {
		super(AiErrorCode.AI_TRIAL_PROVIDER_UNAVAILABLE, AiErrorCode.AI_TRIAL_PROVIDER_UNAVAILABLE.message(), cause);
	}
}
