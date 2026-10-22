package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidMediaObjectKeyException extends DomainException {

	public InvalidMediaObjectKeyException(String value) {
		super(MediaErrorCode.MEDIA_INVALID_OBJECT_KEY, "미디어 객체 키가 올바르지 않습니다: " + value);
	}
}
