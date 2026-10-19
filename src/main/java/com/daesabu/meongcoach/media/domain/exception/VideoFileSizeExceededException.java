package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class VideoFileSizeExceededException extends DomainException {

	public VideoFileSizeExceededException(long bytes, long maxBytes) {
		super(MediaErrorCode.MEDIA_VIDEO_FILE_SIZE_EXCEEDED,
				"영상 파일 크기가 허용 범위를 초과했습니다: " + bytes + " (최대 " + maxBytes + ")");
	}
}
