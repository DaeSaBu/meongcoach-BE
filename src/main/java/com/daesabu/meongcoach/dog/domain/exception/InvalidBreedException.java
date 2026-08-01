package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidBreedException extends DomainException {

	public InvalidBreedException(String value) {
		super(DogErrorCode.DOG_INVALID_BREED, "강아지 견종이 올바르지 않습니다: " + value);
	}
}
