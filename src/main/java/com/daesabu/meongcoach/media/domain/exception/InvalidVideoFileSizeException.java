package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidVideoFileSizeException extends DomainException {

	public InvalidVideoFileSizeException(long bytes) {
		super(MediaErrorCode.MEDIA_INVALID_VIDEO_FILE_SIZE, "영상 파일 크기는 1바이트 이상이어야 합니다: " + bytes);
	}
}
