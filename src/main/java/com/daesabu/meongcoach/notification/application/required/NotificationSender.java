package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import java.util.List;

/** FID 목록에 메시지를 보내고 무효 FID와 첫 실패 예외를 반환한다. */
public interface NotificationSender {
	NotificationSendResult send(List<String> fids, NotificationMessage message);
}
