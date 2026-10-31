package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import jakarta.validation.Valid;

/** 예약 시각의 알림을 한 번 발송한다. */
public interface NotificationDispatcher {
	NotificationDispatchResult dispatch(@Valid NotificationDispatchRequest request);
}
