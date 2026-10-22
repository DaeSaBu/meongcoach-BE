package com.daesabu.meongcoach.media.domain;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;

public record MediaFileSize(Long bytes, long maxBytes) {

	public MediaFileSize {
		if (bytes == null || bytes <= 0) {
			throw new InvalidVideoFileSizeException(bytes);
		}
		if (bytes > maxBytes) {
			throw new VideoFileSizeExceededException(bytes, maxBytes);
		}
	}
}
