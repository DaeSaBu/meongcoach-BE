package com.daesabu.meongcoach.loadtest.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 부하 테스트 공유 키 헤더가 없거나 설정값과 다른 경우.
 */
public class InvalidLoadTestKeyException extends DomainException {

	public InvalidLoadTestKeyException() {
		super(LoadTestErrorCode.LOADTEST_INVALID_KEY);
	}
}
