package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.application.provided.dto.EventNotificationDispatchRequest;
import jakarta.validation.Valid;

/**
 * 사건이 시작하는 이벤트 알림을 발송한다.
 */
public interface EventNotificationDispatcher {

	/**
	 * 대상 회원 중 해당 종류를 켠 활성 회원에게 호출마다 발송한다. 중복 호출을 막지 않는다.
	 * 종류·대상·딥링크가 잘못되면 각각 {@code InvalidNotificationEventTypeException},
	 * {@code InvalidNotificationTargetException}, {@code InvalidNotificationDeepLinkException}을 던진다.
	 */
	EventNotificationDispatchResult dispatch(@Valid EventNotificationDispatchRequest request);
}
