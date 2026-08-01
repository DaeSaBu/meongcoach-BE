package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class VideoFileSizeExceededException extends DomainException {

	// 클라이언트가 재시도 가능 여부를 판단할 수 있도록 허용 상한을 함께 알려 준다
	public VideoFileSizeExceededException(long bytes, long maxBytes) {
		super(MediaErrorCode.MEDIA_VIDEO_FILE_SIZE_EXCEEDED,
				"영상 파일 크기가 허용 범위를 초과했습니다: " + bytes + " (최대 " + maxBytes + ")");
	}
}
