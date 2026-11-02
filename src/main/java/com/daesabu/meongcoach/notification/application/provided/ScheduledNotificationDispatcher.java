package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.application.provided.dto.ScheduledNotificationDispatchRequest;
import jakarta.validation.Valid;

/**
 * 회원이 켠 시각의 정기 알림을 예약 시각마다 한 번 발송한다.
 */
public interface ScheduledNotificationDispatcher {

	/**
	 * 서울 기준 날짜·시·분의 정기 알림을 발송한다. 같은 예약 시각이 진행 중이면 끝날 때까지 기다리고, 완료됐으면 발송 없이 duplicate=true를 반환한다.
	 * 예약 시각이 미래이거나 1시간보다 오래됐으면 {@code InvalidNotificationDispatchTimeException}을 던진다.
	 */
	ScheduledNotificationDispatchResult dispatch(@Valid ScheduledNotificationDispatchRequest request);
}
