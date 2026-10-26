package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidGraduationVideoKeyException extends DomainException {

	public InvalidGraduationVideoKeyException(String objectKey) {
		super(GraduationErrorCode.GRADUATION_INVALID_VIDEO_KEY, "본인이 이 토픽으로 올린 실습 인증 영상의 객체 키가 아닙니다: " + objectKey);
	}
}
