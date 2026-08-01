package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class VideoAccessDeniedException extends DomainException {

	public VideoAccessDeniedException(String objectKey) {
		super(MediaErrorCode.MEDIA_VIDEO_ACCESS_DENIED, "해당 영상에 접근할 권한이 없습니다: " + objectKey);
	}
}
