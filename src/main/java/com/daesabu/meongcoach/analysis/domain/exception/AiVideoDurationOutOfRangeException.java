package com.daesabu.meongcoach.analysis.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class AiVideoDurationOutOfRangeException extends DomainException {

	public AiVideoDurationOutOfRangeException() {
		super(AiErrorCode.AI_VIDEO_DURATION_OUT_OF_RANGE);
	}
}
