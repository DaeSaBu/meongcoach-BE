package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

// 강아지가 한 마리도 없는 상태를 막기 위해 마지막 한 마리는 삭제할 수 없다
public class LastDogNotDeletableException extends DomainException {

	public LastDogNotDeletableException() {
		super(DogErrorCode.DOG_LAST_DOG_NOT_DELETABLE);
	}
}
