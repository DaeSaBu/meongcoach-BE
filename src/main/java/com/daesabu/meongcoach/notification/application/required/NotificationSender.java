package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationSendResult;
import java.util.List;

/** FID 목록에 시각별 알림을 보내고 무효 FID와 실패 수, 첫 실패 예외를 반환한다. */
public interface NotificationSender {
	NotificationSendResult send(List<String> fids, int hour);
}
