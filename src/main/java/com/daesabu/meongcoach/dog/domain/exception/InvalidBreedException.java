package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidBreedException extends DomainException {

	public InvalidBreedException() {
		super(DogErrorCode.DOG_INVALID_BREED);
	}
}
