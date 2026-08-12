package com.daesabu.meongcoach.dog.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class DogNotFoundException extends DomainException {

	// 남의 강아지를 조회한 경우도 존재 여부를 숨기기 위해 같은 미존재 메시지를 쓴다
	public DogNotFoundException(Long dogId) {
		super(DogErrorCode.DOG_NOT_FOUND, "id가 " + dogId + "인 강아지를 찾을 수 없습니다.");
	}
}
