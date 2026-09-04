package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidVideoObjectKeyException extends DomainException {

	public InvalidVideoObjectKeyException(String value) {
		super(MediaErrorCode.MEDIA_INVALID_VIDEO_OBJECT_KEY, "영상 객체 키가 올바르지 않습니다: " + value);
	}
}
