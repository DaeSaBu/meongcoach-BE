package com.daesabu.meongcoach.media.domain;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;

public record CommunityVideoFileSize(long bytes) {

	public static final long MAX_BYTES = 512L * 1024 * 1024;

	public CommunityVideoFileSize {
		if (bytes <= 0) {
			throw new InvalidVideoFileSizeException(bytes);
		}
		if (bytes > MAX_BYTES) {
			throw new VideoFileSizeExceededException(bytes, MAX_BYTES);
		}
	}
}
