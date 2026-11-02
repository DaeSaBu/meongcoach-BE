package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.PushNotification;
import java.util.List;

/**
 * 알림을 외부 API의 배치 제한에 맞춰 나누어 발송하고 알림별 결과를 반환한다.
 * 개별 알림의 실패와 무효 FID를 결과에 기록하며, 배치 호출이 실패하면 남은 알림을 보내지 않고 원인과 함께 중단으로 기록한다.
 */
public interface NotificationSender {
	NotificationSendResult send(List<PushNotification> notifications);
}
