package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.PushNotification;
import java.util.List;
import java.util.Optional;

/**
 * 알림을 외부 API의 배치 제한에 맞춰 나누어 발송하고 배치별 결과를 순서대로 반환한다.
 * 개별 알림 실패와 배치 호출 실패를 결과에 기록하며, 한 배치가 실패해도 남은 배치를 계속 보낸다.
 */
public interface NotificationSender {
	List<NotificationBatchResult> send(List<PushNotification> notifications);

	interface NotificationBatchResult {
		/** 전송에 성공한 알림 수를 반환한다. */
		int sentCount();

		/** 무효 등록 외의 이유로 전송하지 못한 알림 수를 반환한다. 배치 호출이 실패하면 배치 전체를 센다. */
		int failedCount();

		/** 무효로 판정된 FID 목록을 반환한다. */
		List<String> invalidFids();

		/** 배치 호출 자체가 실패한 원인을 반환한다. */
		Optional<Throwable> failure();
	}
}
