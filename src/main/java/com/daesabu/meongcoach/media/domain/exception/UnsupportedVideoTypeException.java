package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class UnsupportedVideoTypeException extends DomainException {

	public UnsupportedVideoTypeException(String contentType) {
		super(MediaErrorCode.MEDIA_UNSUPPORTED_VIDEO_TYPE, "지원하지 않는 영상 형식입니다: " + contentType);
	}
}
