package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class VideoSizeExceededException extends DomainException {

	public VideoSizeExceededException(long bytes) {
		super(MediaErrorCode.MEDIA_VIDEO_SIZE_EXCEEDED, "영상 크기가 허용 범위를 벗어났습니다: " + bytes + " bytes");
	}
}
