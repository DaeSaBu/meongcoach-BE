package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidUploadTargetException extends DomainException {

	public InvalidUploadTargetException(String value) {
		super(MediaErrorCode.MEDIA_INVALID_UPLOAD_TARGET, "이미지 업로드 대상이 올바르지 않습니다: " + value);
	}
}
