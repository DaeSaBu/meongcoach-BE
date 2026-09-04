package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

// 사용자당 등록할 수 있는 강아지 수를 넘겨 더 등록할 수 없다
public class DogLimitExceededException extends DomainException {

	public DogLimitExceededException() {
		super(DogErrorCode.DOG_LIMIT_EXCEEDED);
	}
}
