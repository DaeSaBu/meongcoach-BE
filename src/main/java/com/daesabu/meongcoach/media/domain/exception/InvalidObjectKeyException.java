package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidObjectKeyException extends DomainException {

	public InvalidObjectKeyException(String value) {
		super(MediaErrorCode.MEDIA_INVALID_OBJECT_KEY, "영상 객체 키가 올바르지 않습니다: " + value);
	}
}
