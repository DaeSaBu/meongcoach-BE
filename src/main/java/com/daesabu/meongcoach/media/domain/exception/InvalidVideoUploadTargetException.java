package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidVideoUploadTargetException extends DomainException {

	public InvalidVideoUploadTargetException(String value) {
		super(MediaErrorCode.MEDIA_INVALID_VIDEO_UPLOAD_TARGET, "영상 업로드 대상이 올바르지 않습니다: " + value);
	}
}
