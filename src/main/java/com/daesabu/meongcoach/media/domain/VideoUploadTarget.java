package com.daesabu.meongcoach.media.domain;

import com.daesabu.meongcoach.media.domain.exception.InvalidVideoUploadTargetException;

/**
 * 영상 업로드 대상. 객체 키의 경로 구획을 결정해 대상별로 영상을 분리 보관한다.
 */
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

	/**
	 * 문자열 코드를 enum으로 변환한다. 실패 시 우리 에러 코드를 유지하기 위해 도메인에서 직접 변환한다.
	 */
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
