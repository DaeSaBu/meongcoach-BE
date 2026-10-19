package com.daesabu.meongcoach.media.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;
import java.util.Map;

public class VideoFileSizeExceededException extends DomainException {

	private final long maxFileSizeBytes;

	public VideoFileSizeExceededException(long bytes, long maxBytes) {
		super(MediaErrorCode.MEDIA_VIDEO_FILE_SIZE_EXCEEDED,
				"영상 파일 크기가 허용 범위를 초과했습니다: " + bytes + " (최대 " + maxBytes + ")");
		this.maxFileSizeBytes = maxBytes;
	}

	@Override
	public Map<String, Object> getProperties() {
		return Map.of("maxFileSizeBytes", maxFileSizeBytes);
	}
}
