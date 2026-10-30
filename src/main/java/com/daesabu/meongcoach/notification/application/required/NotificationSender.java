package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import java.util.List;
import java.util.Map;

/**
 * FID별 메시지를 외부 API의 배치 제한에 맞춰 발송하고 배치별 결과를 순서대로 반환한다.
 * 무효 등록 외 발송 실패가 발생하면 NotificationDeliveryException을 던지고 후속 배치를 중단한다.
 */
public interface NotificationSender {
	List<BatchResult> send(Map<String, NotificationMessage> messages);

	interface BatchResult {
		/** 무효로 판정된 FID 목록을 반환한다. */
		List<String> invalidFids();
	}
}
