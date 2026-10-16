package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class BreedLengthExceededException extends DomainException {

	public BreedLengthExceededException() {
		super(DogErrorCode.DOG_BREED_LENGTH_EXCEEDED);
	}
}
