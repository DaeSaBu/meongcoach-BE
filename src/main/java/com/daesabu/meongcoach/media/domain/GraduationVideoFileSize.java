package com.daesabu.meongcoach.media.domain;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoFileSizeException;
import com.daesabu.meongcoach.media.domain.exception.VideoFileSizeExceededException;

public final class GraduationVideoFileSize {

	private final long bytes;

	public GraduationVideoFileSize(long bytes, long maxBytes) {
		if (bytes <= 0) {
			throw new InvalidVideoFileSizeException(bytes);
		}
		if (bytes > maxBytes) {
			throw new VideoFileSizeExceededException(bytes, maxBytes);
		}
		this.bytes = bytes;
	}

	public long bytes() {
		return bytes;
	}
}
