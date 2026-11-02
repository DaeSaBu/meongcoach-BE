package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.daesabu.meongcoach.notification.domain.exception.NotificationDeliveryException;
import java.util.List;
import java.util.Objects;

/**
 * 알림을 외부 API의 배치 제한에 맞춰 나누어 발송하고 합친 결과를 반환한다.
 * 개별 알림 실패와 배치 호출 실패를 결과에 기록하며, 한 배치가 실패해도 남은 배치를 계속 보낸다.
 */
public interface NotificationSender {
	NotificationSendResult send(List<PushNotification> notifications);

	record NotificationSendResult(int sentCount, int failedCount, List<String> invalidFids, Throwable failure) {

		public NotificationSendResult {
			invalidFids = List.copyOf(invalidFids);
		}

		public static NotificationSendResult merge(List<NotificationSendResult> results) {
			int sentCount = results.stream().mapToInt(NotificationSendResult::sentCount).sum();
			int failedCount = results.stream().mapToInt(NotificationSendResult::failedCount).sum();
			List<String> invalidFids = results.stream().flatMap(result -> result.invalidFids().stream()).toList();
			List<Throwable> failures = results.stream().map(NotificationSendResult::failure).filter(Objects::nonNull).toList();
			Throwable failure = null;
			for (Throwable each : failures) {
				if (failure == null) {
					failure = each;
					continue;
				}
				failure.addSuppressed(each);
			}
			return new NotificationSendResult(sentCount, failedCount, invalidFids, failure);
		}

		public void requireDelivered() {
			if (failure != null) {
				throw new NotificationDeliveryException(failure);
			}
			if (sentCount == 0 && failedCount > 0) {
				throw new NotificationDeliveryException();
			}
		}
	}
}
