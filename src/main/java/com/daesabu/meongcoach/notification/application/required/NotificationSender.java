package com.daesabu.meongcoach.notification.application.required;

import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import java.util.List;

/** FID 목록을 외부 API의 배치 제한에 맞춰 발송하고 배치별 발송 결과를 순서대로 반환한다. */
public interface NotificationSender {
	List<NotificationBatchResult> send(List<String> fids, NotificationMessage message);
}
