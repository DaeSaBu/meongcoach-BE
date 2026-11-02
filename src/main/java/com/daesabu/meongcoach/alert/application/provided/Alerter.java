package com.daesabu.meongcoach.alert.application.provided;

import com.daesabu.meongcoach.alert.application.provided.dto.AlertRequest;
import jakarta.validation.Valid;

/**
 * 개발자 알림 공개 API.
 */
public interface Alerter {

	/**
	 * 알림을 개발자 채널로 비동기 전송한다.
	 * 요청 검증과 전송은 비동기 스레드에서 수행하며, 검증 위반과 전송 실패는 호출자에게 전파되지 않고 error 로그로 남는다.
	 */
	void alert(@Valid AlertRequest request);
}
