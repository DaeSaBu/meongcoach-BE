package com.daesabu.meongcoach.alert.application.provided;

import com.daesabu.meongcoach.alert.application.provided.dto.AlertRequest;
import jakarta.validation.Valid;

/**
 * 개발자 알림 공개 API.
 */
public interface Alerter {

	/**
	 * 알림을 개발자 채널로 비동기 전송한다.
	 * 요청 검증은 호출 스레드에서 수행하고, 위반이 있으면 {@code ConstraintViolationException}을 던지고 전송하지 않는다.
	 * 전송 실패는 호출자에게 전파되지 않는다.
	 */
	void alert(@Valid AlertRequest request);
}
