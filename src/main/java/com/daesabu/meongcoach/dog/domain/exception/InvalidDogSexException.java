package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidDogSexException extends DomainException {

	public InvalidDogSexException(String value) {
		super(DogErrorCode.DOG_INVALID_SEX, "강아지 성별이 올바르지 않습니다: " + value);
	}
}
