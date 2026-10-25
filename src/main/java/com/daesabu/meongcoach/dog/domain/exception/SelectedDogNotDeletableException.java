package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class SelectedDogNotDeletableException extends DomainException {

	public SelectedDogNotDeletableException() {
		super(DogErrorCode.DOG_SELECTED_DOG_NOT_DELETABLE);
	}
}
