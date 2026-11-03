package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationDelivery;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import java.util.List;

/**
 * 알림을 외부 API의 배치 제한에 맞춰 나누어 보내고 전달 결과를 반환한다.
 * 전달된 수, 실패한 수, 수신 불가로 판정된 FID를 결과에 담는다. 전송 호출이 실패하면 NotificationDeliveryException을 던진다.
 */
public interface NotificationSender {
	NotificationDelivery send(List<PushNotification> notifications);
}
