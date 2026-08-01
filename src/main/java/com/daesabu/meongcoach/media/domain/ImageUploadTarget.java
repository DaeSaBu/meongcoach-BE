package com.daesabu.meongcoach.media.domain;

import com.daesabu.meongcoach.media.domain.exception.InvalidUploadTargetException;

/**
 * 이미지 업로드 대상. 객체 키의 경로 구획을 결정해 대상별로 이미지를 분리 보관한다.
 */
public enum ImageUploadTarget {
	USER_PROFILE("user-profile"),
	DOG_PROFILE("dog-profile"),
	;

	private final String pathSegment;

	ImageUploadTarget(String pathSegment) {
		this.pathSegment = pathSegment;
	}

	public String getPathSegment() {
		return pathSegment;
	}

	/**
	 * 문자열 코드를 enum으로 변환한다. 실패 시 우리 에러 코드를 유지하기 위해 도메인에서 직접 변환한다.
	 */
	public static ImageUploadTarget from(String value) {
		if (value == null) {
			throw new InvalidUploadTargetException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidUploadTargetException(value);
		}
	}
}
