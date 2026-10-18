package com.daesabu.meongcoach.media.domain;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoUploadTargetException;

public enum VideoUploadTarget {
	TRAINING_VIDEO("training", false),
	// 실습 인증 영상은 토픽 단위로 올리므로 사용자 ID 다음에 토픽 ID를 묶음 ID로 둔다
	GRADUATION_VIDEO("graduation", true),
	;

	private final String pathSegment;
	private final boolean scoped;

	VideoUploadTarget(String pathSegment, boolean scoped) {
		this.pathSegment = pathSegment;
		this.scoped = scoped;
	}

	public String getPathSegment() {
		return pathSegment;
	}

	/**
	 * 객체 키에 사용자 ID 다음 단계로 묶음 ID가 필요한 대상이면 참이다.
	 */
	public boolean isScoped() {
		return scoped;
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
