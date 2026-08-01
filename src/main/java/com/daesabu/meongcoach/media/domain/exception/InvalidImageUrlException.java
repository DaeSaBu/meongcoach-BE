package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidImageUrlException extends DomainException {

	public InvalidImageUrlException() {
		super(MediaErrorCode.MEDIA_INVALID_IMAGE_URL);
	}
}
