package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class DogNotFoundException extends DomainException {

	public DogNotFoundException() {
		super(DogErrorCode.DOG_NOT_FOUND, "선택된 강아지를 찾을 수 없습니다.");
	}
}
