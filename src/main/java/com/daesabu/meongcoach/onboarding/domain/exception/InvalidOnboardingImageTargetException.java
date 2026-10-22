package com.daesabu.meongcoach.onboarding.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidOnboardingImageTargetException extends DomainException {

	public InvalidOnboardingImageTargetException(String value) {
		super(OnboardingErrorCode.ONBOARDING_INVALID_IMAGE_UPLOAD_TARGET, "이미지 업로드 대상이 올바르지 않습니다: " + value);
	}
}
