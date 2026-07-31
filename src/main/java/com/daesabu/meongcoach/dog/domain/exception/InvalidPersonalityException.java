package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidPersonalityException extends DomainException {

	public InvalidPersonalityException(String value) {
		super(DogErrorCode.DOG_INVALID_PERSONALITY, "강아지 성격이 올바르지 않습니다: " + value);
	}
}
