package com.daesabu.meongcoach.media.domain;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoUploadTargetException;

public enum VideoUploadTarget {
	TRAINING_VIDEO("training"),
	;

	private final String pathSegment;

	VideoUploadTarget(String pathSegment) {
		this.pathSegment = pathSegment;
	}

	public String getPathSegment() {
		return pathSegment;
	}

	public static VideoUploadTarget from(String value) {
		if (value == null) {
			throw new InvalidVideoUploadTargetException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidVideoUploadTargetException(value);
		}
	}
}
