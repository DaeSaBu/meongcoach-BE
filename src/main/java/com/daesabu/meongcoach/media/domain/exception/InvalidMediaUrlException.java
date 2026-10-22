package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidMediaUrlException extends DomainException {

	public InvalidMediaUrlException() {
		super(MediaErrorCode.MEDIA_INVALID_URL);
	}
}
