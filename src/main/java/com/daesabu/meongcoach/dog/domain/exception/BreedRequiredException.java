package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class BreedRequiredException extends DomainException {

	public BreedRequiredException() {
		super(DogErrorCode.DOG_BREED_REQUIRED);
	}
}
