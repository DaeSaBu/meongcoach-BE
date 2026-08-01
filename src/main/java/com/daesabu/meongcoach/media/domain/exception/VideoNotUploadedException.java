package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class VideoNotUploadedException extends DomainException {

	public VideoNotUploadedException(String objectKey) {
		super(MediaErrorCode.MEDIA_VIDEO_NOT_UPLOADED, "업로드되지 않은 영상입니다: " + objectKey);
	}
}
