package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class UnsupportedImageTypeException extends DomainException {

	public UnsupportedImageTypeException(String contentType) {
		super(MediaErrorCode.MEDIA_UNSUPPORTED_IMAGE_TYPE, "지원하지 않는 이미지 형식입니다: " + contentType);
	}
}
